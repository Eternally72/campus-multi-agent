package com.campus.agent.document;

import com.campus.agent.course.Course;
import com.campus.agent.course.CourseService;
import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentService {

    private final CourseMaterialRepository materials;
    private final AppUserRepository users;
    private final CourseService courseService;
    private final DocumentProperties properties;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentIndexingService indexingService;
    private final DocumentTextExtractor textExtractor;

    @Transactional
    public MaterialResponse create(Long userId, CreateMaterialRequest request) {
        return createMaterial(userId, request.courseId(), request.title(), request.content());
    }

    @Transactional
    public MaterialResponse createFromFile(Long userId, Long courseId, String title, org.springframework.web.multipart.MultipartFile file) {
        DocumentTextExtractor.ExtractedDocument extracted = textExtractor.extract(file);
        String resolvedTitle = title == null || title.isBlank() ? extracted.filename() : title;
        return createMaterial(userId, courseId, resolvedTitle, extracted.text());
    }

    private MaterialResponse createMaterial(Long userId, Long courseId, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("资料标题不能为空");
        }
        if (content.length() > properties.maxCharacters()) {
            throw new IllegalArgumentException("资料内容过长，请拆分后上传");
        }
        AppUser owner = users.getReferenceById(userId);
        Course course = courseId == null ? null : courseService.getOwnedCourse(userId, courseId);

        CourseMaterial material = new CourseMaterial();
        material.setOwner(owner);
        material.setCourse(course);
        material.setTitle(title);
        material.setContent(content);
        material.setStatus(MaterialStatus.PENDING);
        materials.save(material);
        publishAfterCommit(new DocumentIndexEvent(userId, material.getId(), course == null ? null : course.getId(), material.getTitle()));

        return MaterialResponse.from(material);
    }

    @Transactional
    public MaterialResponse reindex(Long userId, Long materialId) {
        CourseMaterial material = ownedMaterial(userId, materialId);
        material.setStatus(MaterialStatus.PENDING);
        material.setErrorMessage(null);
        publishAfterCommit(new DocumentIndexEvent(userId, material.getId(), material.getCourse() == null ? null : material.getCourse().getId(), material.getTitle()));
        return MaterialResponse.from(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(Long userId) {
        return materials.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
            .map(MaterialResponse::from)
            .toList();
    }

    @Transactional
    public void delete(Long userId, Long materialId) {
        CourseMaterial material = ownedMaterial(userId, materialId);
        indexingService.deleteVectors(userId, material.getId());
        materials.delete(material);
    }

    private CourseMaterial ownedMaterial(Long userId, Long materialId) {
        CourseMaterial material = materials.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("资料不存在"));
        if (!material.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该资料");
        }
        return material;
    }

    private void publishAfterCommit(DocumentIndexEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    eventPublisher.publishIndexRequested(event);
                } catch (RuntimeException exception) {
                    indexingService.markFailed(event.materialId(), "索引任务投递失败：" + exception.getMessage());
                }
            }
        });
    }
}
