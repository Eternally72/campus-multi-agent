package com.campus.agent.memory;

import com.campus.agent.agent.ChatMessage;
import com.campus.agent.agent.ChatSession;
import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ConversationSummaryRepository summaries;
    private final UserMemoryPreferenceRepository preferences;
    private final UserMemoryFactRepository facts;
    private final MemoryCandidateRepository candidates;
    private final MemoryExtractionService extractionService;
    private final AppUserRepository users;
    private final MemoryProperties properties;

    @Transactional(readOnly = true)
    public MemoryContext buildContext(Long userId, Long sessionId) {
        List<String> shortTerm = readShortTerm(userId, sessionId);
        String mediumSummary = summaries.findBySessionId(sessionId)
            .map(ConversationSummary::getSummary)
            .orElse("");
        List<String> longTerm = preferences.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, MemoryStatus.ACTIVE).stream()
            .map(preference -> preference.getMemoryKey() + "=" + preference.getMemoryValue())
            .limit(12)
            .toList();
        Instant now = Instant.now();
        List<String> activeFacts = facts.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, MemoryStatus.ACTIVE).stream()
            .filter(fact -> fact.getExpiresAt() == null || fact.getExpiresAt().isAfter(now))
            .map(fact -> fact.getCategory() + ":" + fact.getFactValue())
            .limit(12)
            .toList();
        return new MemoryContext(shortTerm == null ? List.of() : shortTerm, mediumSummary, longTerm, activeFacts);
    }

    @Transactional
    public void afterTurn(Long userId, ChatSession session, ChatMessage userMessage, ChatMessage assistantMessage) {
        appendShortTerm(userId, session.getId(), "user: " + userMessage.getContent());
        appendShortTerm(userId, session.getId(), "assistant: " + assistantMessage.getContent());
        updateMediumSummary(userId, session, assistantMessage);
        createMemoryCandidates(userId, session, userMessage.getContent());
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> list(Long userId) {
        return Stream.concat(
            preferences.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, MemoryStatus.ACTIVE).stream().map(MemoryResponse::from),
            facts.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, MemoryStatus.ACTIVE).stream().map(MemoryResponse::from)
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<MemoryCandidateResponse> listCandidates(Long userId) {
        return candidates.findByUserIdAndStatusOrderByCreatedAtDesc(userId, MemoryCandidateStatus.PENDING).stream()
            .map(MemoryCandidateResponse::from)
            .toList();
    }

    @Transactional
    public void confirmCandidate(Long userId, Long candidateId) {
        MemoryCandidate candidate = ownedCandidate(userId, candidateId);
        AppUser user = candidate.getUser();
        ChatSession session = candidate.getSession();
        if (candidate.getMemoryType() == MemoryType.PREFERENCE) {
            upsertPreference(user, session, new PreferenceCandidate(
                candidate.getMemoryKey(),
                candidate.getMemoryValue(),
                candidate.getConfidence(),
                candidate.getSource()
            ));
        } else {
            upsertFact(user, session, new FactCandidate(
                candidate.getCategory() == null ? "fact" : candidate.getCategory(),
                candidate.getMemoryKey(),
                candidate.getMemoryValue(),
                candidate.getConfidence(),
                candidate.getSource(),
                candidate.getExpiresAt()
            ));
        }
        candidate.setStatus(MemoryCandidateStatus.CONFIRMED);
        candidate.setDecidedAt(Instant.now());
    }

    @Transactional
    public void rejectCandidate(Long userId, Long candidateId) {
        MemoryCandidate candidate = ownedCandidate(userId, candidateId);
        candidate.setStatus(MemoryCandidateStatus.REJECTED);
        candidate.setDecidedAt(Instant.now());
    }

    @Transactional
    public void forgetPreference(Long userId, Long id) {
        UserMemoryPreference preference = preferences.findById(id).orElseThrow(() -> new IllegalArgumentException("偏好记忆不存在"));
        if (!preference.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该记忆");
        }
        markForgotten(preference, null);
    }

    @Transactional
    public void forgetFact(Long userId, Long id) {
        UserMemoryFact fact = facts.findById(id).orElseThrow(() -> new IllegalArgumentException("事实记忆不存在"));
        if (!fact.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该记忆");
        }
        markForgotten(fact, null);
    }

    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void expireFacts() {
        Instant now = Instant.now();
        facts.findByStatusAndExpiresAtBefore(MemoryStatus.ACTIVE, now)
            .forEach(fact -> markForgotten(fact, null));
    }

    private void appendShortTerm(Long userId, Long sessionId, String value) {
        String key = shortKey(userId, sessionId);
        try {
            redisTemplate.opsForList().rightPush(key, truncate(value, 600));
            redisTemplate.opsForList().trim(key, -properties.shortTermMaxMessages(), -1);
            redisTemplate.expire(key, Duration.ofMinutes(properties.shortTermTtlMinutes()));
        } catch (RuntimeException ignored) {
            // Short-term memory is a cache. Redis outages should not block the core chat flow.
        }
    }

    private List<String> readShortTerm(Long userId, Long sessionId) {
        try {
            List<String> values = redisTemplate.opsForList().range(shortKey(userId, sessionId), 0, -1);
            return values == null ? List.of() : values;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private void updateMediumSummary(Long userId, ChatSession session, ChatMessage assistantMessage) {
        ConversationSummary summary = summaries.findBySessionId(session.getId()).orElseGet(() -> {
            ConversationSummary created = new ConversationSummary();
            created.setUser(users.getReferenceById(userId));
            created.setSession(session);
            created.setSummary("");
            return created;
        });
        String previous = summary.getSummary() == null ? "" : summary.getSummary();
        String addition = "最近进展：用户继续围绕“" + session.getTitle() + "”对话；系统最近回答：" + truncate(assistantMessage.getContent(), 220);
        summary.setSummary(truncate((previous + "\n" + addition).trim(), properties.summaryMaxCharacters()));
        summary.setKeyPoints(extractKeyPoints(summary.getSummary()));
        summary.setOpenTasks(extractOpenTasks(summary.getSummary()));
        summary.setLastMessageId(assistantMessage.getId());
        summary.setUpdatedAt(Instant.now());
        summaries.save(summary);
    }

    private void createMemoryCandidates(Long userId, ChatSession session, String message) {
        AppUser user = users.getReferenceById(userId);
        MemoryContext context = buildContext(userId, session.getId());
        List<MemoryExtractionCandidate> extracted = extractionService.extract(message, context);
        List<MemoryExtractionCandidate> finalCandidates = extracted.isEmpty() ? ruleCandidates(message) : extracted;
        for (MemoryExtractionCandidate candidate : finalCandidates) {
            createCandidate(user, session, candidate);
        }
    }

    private void createCandidate(AppUser user, ChatSession session, MemoryExtractionCandidate candidate) {
        if (candidate.confidence() < 0.55) {
            return;
        }
        if (candidates.findFirstByUserIdAndMemoryTypeAndMemoryKeyAndMemoryValueAndStatusOrderByCreatedAtDesc(
            user.getId(), candidate.type(), candidate.key(), candidate.value(), MemoryCandidateStatus.PENDING
        ).isPresent()) {
            return;
        }
        MemoryCandidate memoryCandidate = new MemoryCandidate();
        memoryCandidate.setUser(user);
        memoryCandidate.setSession(session);
        memoryCandidate.setMemoryType(candidate.type());
        memoryCandidate.setMemoryKey(candidate.key());
        memoryCandidate.setMemoryValue(truncate(candidate.value(), 1000));
        memoryCandidate.setCategory(candidate.category());
        memoryCandidate.setConfidence(candidate.confidence());
        memoryCandidate.setReason(truncate(candidate.reason(), 500));
        memoryCandidate.setSource(candidate.source());
        if (candidate.type() == MemoryType.FACT) {
            int ttl = candidate.ttlDays() == null ? properties.factDefaultTtlDays() : Math.max(1, candidate.ttlDays());
            memoryCandidate.setExpiresAt(Instant.now().plus(ttl, ChronoUnit.DAYS));
        }
        candidates.save(memoryCandidate);
    }

    private void upsertPreference(AppUser user, ChatSession session, PreferenceCandidate candidate) {
        Optional<UserMemoryPreference> existing = preferences.findFirstByUserIdAndMemoryKeyAndStatusOrderByUpdatedAtDesc(
            user.getId(), candidate.key(), MemoryStatus.ACTIVE);
        if (existing.isPresent() && existing.get().getMemoryValue().equals(candidate.value())) {
            UserMemoryPreference preference = existing.get();
            preference.setConfidence(Math.min(1.0, Math.max(preference.getConfidence(), candidate.confidence())));
            preference.setUpdatedAt(Instant.now());
            return;
        }

        UserMemoryPreference created = new UserMemoryPreference();
        created.setUser(user);
        created.setSourceSession(session);
        created.setMemoryKey(candidate.key());
        created.setMemoryValue(candidate.value());
        created.setConfidence(candidate.confidence());
        created.setSource("rule:" + candidate.reason());
        preferences.save(created);
        existing.ifPresent(old -> markForgotten(old, created));
    }

    private void upsertFact(AppUser user, ChatSession session, FactCandidate candidate) {
        Optional<UserMemoryFact> existing = facts.findFirstByUserIdAndFactKeyAndStatusOrderByUpdatedAtDesc(
            user.getId(), candidate.key(), MemoryStatus.ACTIVE);
        if (existing.isPresent() && existing.get().getFactValue().equals(candidate.value())) {
            UserMemoryFact fact = existing.get();
            fact.setConfidence(Math.min(1.0, Math.max(fact.getConfidence(), candidate.confidence())));
            fact.setUpdatedAt(Instant.now());
            return;
        }

        UserMemoryFact created = new UserMemoryFact();
        created.setUser(user);
        created.setSourceSession(session);
        created.setCategory(candidate.category());
        created.setFactKey(candidate.key());
        created.setFactValue(candidate.value());
        created.setConfidence(candidate.confidence());
        created.setExpiresAt(candidate.expiresAt() == null ? Instant.now().plus(properties.factDefaultTtlDays(), ChronoUnit.DAYS) : candidate.expiresAt());
        created.setSource("rule:" + candidate.reason());
        facts.save(created);
        existing.ifPresent(old -> markForgotten(old, created));
    }

    private List<PreferenceCandidate> preferenceCandidates(String message) {
        String text = normalize(message);
        if (!containsAny(text, "以后", "请记住", "记住", "我希望", "回答我", "偏好")) {
            return List.of();
        }
        List<PreferenceCandidate> result = new ArrayList<>();
        if (containsAny(text, "简洁", "短一点", "少一点")) {
            result.add(new PreferenceCandidate("answer_style", "concise", 0.86, "explicit-style"));
        }
        if (containsAny(text, "详细", "展开", "多解释")) {
            result.add(new PreferenceCandidate("answer_style", "detailed", 0.86, "explicit-style"));
        }
        if (containsAny(text, "举例", "例子", "案例")) {
            result.add(new PreferenceCandidate("prefers_examples", "true", 0.82, "explicit-example"));
        }
        if (containsAny(text, "中文", "用中文")) {
            result.add(new PreferenceCandidate("language", "zh-CN", 0.9, "explicit-language"));
        }
        if (containsAny(text, "英文", "用英文")) {
            result.add(new PreferenceCandidate("language", "en", 0.9, "explicit-language"));
        }
        if (containsAny(text, "轻松", "活泼", "友好")) {
            result.add(new PreferenceCandidate("tone", "friendly", 0.76, "explicit-tone"));
        }
        if (containsAny(text, "严谨", "正式")) {
            result.add(new PreferenceCandidate("tone", "formal", 0.76, "explicit-tone"));
        }
        return result;
    }

    private List<MemoryExtractionCandidate> ruleCandidates(String message) {
        List<MemoryExtractionCandidate> result = new ArrayList<>();
        preferenceCandidates(message).forEach(candidate -> result.add(new MemoryExtractionCandidate(
            MemoryType.PREFERENCE,
            candidate.key(),
            candidate.value(),
            "preference",
            candidate.confidence(),
            candidate.reason(),
            null,
            "rule"
        )));
        factCandidates(message).forEach(candidate -> result.add(new MemoryExtractionCandidate(
            MemoryType.FACT,
            candidate.key(),
            candidate.value(),
            candidate.category(),
            candidate.confidence(),
            candidate.reason(),
            properties.factDefaultTtlDays(),
            "rule"
        )));
        return result;
    }

    private List<FactCandidate> factCandidates(String message) {
        String text = normalize(message);
        List<FactCandidate> result = new ArrayList<>();
        capture(text, "我(?:这学期)?(?:正在)?学(?:习)?([^。；;，,]{2,40})", "current_courses", "course", result);
        capture(text, "我准备([^。；;，,]{2,40})", "study_goal", "goal", result);
        capture(text, "我的目标是([^。；;，,]{2,40})", "study_goal", "goal", result);
        capture(text, "我通常([^。；;，,]{2,40})", "habit", "habit", result);
        capture(text, "我每周([^。；;，,]{2,40})", "schedule", "schedule", result);
        return result;
    }

    private void capture(String text, String regex, String key, String category, List<FactCandidate> result) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) {
            result.add(new FactCandidate(category, key, truncate(matcher.group(1).trim(), 180), 0.72, "explicit-fact", null));
        }
    }

    private MemoryCandidate ownedCandidate(Long userId, Long candidateId) {
        MemoryCandidate candidate = candidates.findById(candidateId)
            .orElseThrow(() -> new IllegalArgumentException("记忆候选不存在"));
        if (!candidate.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该记忆候选");
        }
        if (candidate.getStatus() != MemoryCandidateStatus.PENDING) {
            throw new IllegalArgumentException("该记忆候选已处理");
        }
        return candidate;
    }

    private void markForgotten(UserMemoryPreference preference, UserMemoryPreference replacement) {
        preference.setStatus(MemoryStatus.FORGOTTEN);
        preference.setForgottenAt(Instant.now());
        preference.setUpdatedAt(Instant.now());
        preference.setReplacedBy(replacement);
    }

    private void markForgotten(UserMemoryFact fact, UserMemoryFact replacement) {
        fact.setStatus(MemoryStatus.FORGOTTEN);
        fact.setForgottenAt(Instant.now());
        fact.setUpdatedAt(Instant.now());
        fact.setReplacedBy(replacement);
    }

    private String extractKeyPoints(String summary) {
        return truncate(summary.replace("\n", "；"), 1000);
    }

    private String extractOpenTasks(String summary) {
        return containsAny(summary, "待办", "计划", "作业", "复习") ? "可能存在学习计划或待办事项，请结合用户最新问题确认。" : "";
    }

    private String shortKey(Long userId, Long sessionId) {
        return "memory:short:" + userId + ":" + sessionId;
    }

    private boolean containsAny(String text, String... words) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record PreferenceCandidate(String key, String value, double confidence, String reason) {
    }

    private record FactCandidate(String category, String key, String value, double confidence, String reason, Instant expiresAt) {
    }
}
