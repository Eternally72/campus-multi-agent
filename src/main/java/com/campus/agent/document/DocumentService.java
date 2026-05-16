package com.campus.agent.document;

import com.campus.agent.course.Course;
import com.campus.agent.course.CourseService;
import com.campus.agent.rag.RagService;
import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentService {

    private final CourseMaterialRepository materials;
    private final AppUserRepository users;
    private final CourseService courseService;
    private final RagService ragService;
    private final DocumentProperties properties;
    private final DocumentEventPublisher eventPublisher;

    @Transactional
    public MaterialResponse create(Long userId, CreateMaterialRequest request) {
        if (request.content().length() > properties.maxCharacters()) {
            throw new IllegalArgumentException("资料内容过长，请拆分后上传");
        }

        AppUser owner = users.getReferenceById(userId);
        Course course = request.courseId() == null ? null : courseService.getOwnedCourse(userId, request.courseId());

        CourseMaterial material = new CourseMaterial();
        material.setOwner(owner);
        material.setCourse(course);
        material.setTitle(request.title());
        material.setContent(request.content());
        material.setStatus(MaterialStatus.PENDING);
        materials.save(material);
        eventPublisher.publishIndexRequested(new DocumentIndexEvent(userId, material.getId(), course == null ? null : course.getId(), material.getTitle()));

        try {
            ragService.indexMaterial(userId, material.getId(), course == null ? null : course.getId(), material.getTitle(), material.getContent());
            material.setStatus(MaterialStatus.INDEXED);
            material.setIndexedAt(Instant.now());
            material.setErrorMessage(null);
        } catch (RuntimeException exception) {
            material.setStatus(MaterialStatus.FAILED);
            material.setErrorMessage(exception.getMessage());
        }

        return MaterialResponse.from(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(Long userId) {
        return materials.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
            .map(MaterialResponse::from)
            .toList();
    }
}
