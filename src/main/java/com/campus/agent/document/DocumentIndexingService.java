package com.campus.agent.document;

import com.campus.agent.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DocumentIndexingService {

    private final CourseMaterialRepository materials;
    private final RagService ragService;

    @Transactional
    public void index(DocumentIndexEvent event) {
        CourseMaterial material = materials.findById(event.materialId())
            .orElseThrow(() -> new IllegalArgumentException("资料不存在：" + event.materialId()));
        try {
            Long courseId = material.getCourse() == null ? null : material.getCourse().getId();
            ragService.indexMaterial(material.getOwner().getId(), material.getId(), courseId, material.getTitle(), material.getContent());
            material.setStatus(MaterialStatus.INDEXED);
            material.setIndexedAt(Instant.now());
            material.setErrorMessage(null);
        } catch (RuntimeException exception) {
            material.setStatus(MaterialStatus.FAILED);
            material.setErrorMessage(shortMessage(exception));
        }
    }

    @Transactional
    public void markFailed(Long materialId, String message) {
        materials.findById(materialId).ifPresent(material -> {
            material.setStatus(MaterialStatus.FAILED);
            material.setErrorMessage(shortMessage(message));
        });
    }

    public void deleteVectors(Long userId, Long materialId) {
        ragService.deleteMaterialVectors(userId, materialId);
    }

    private String shortMessage(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return shortMessage(message);
    }

    private String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown error";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
