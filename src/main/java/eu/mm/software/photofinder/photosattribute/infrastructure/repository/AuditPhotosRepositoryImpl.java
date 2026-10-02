package eu.mm.software.photofinder.photosattribute.infrastructure.repository;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotosRepository;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.AuditPhotoSpringDataRepository;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
class AuditPhotosRepositoryImpl implements AuditPhotosRepository {

    private final AuditPhotoSpringDataRepository auditRepository;
    private final PhotoAttributeSpringDataRepository photosRepository;
    private final UserQuery userQuery;
    private final VectorStore vectorStore;

    @Override
    public void removeUnlinkAuditPhotos() {

        List<PhotoAttribute> byUserId = photosRepository.findByUserId(userQuery.findLoggedUserId());
        Set<AuditPhotos> allAuditPhotos = auditRepository.findAllByUserId(userQuery.findLoggedUserId());

        List<String> photosIdsByUser = byUserId.stream()
                .map(PhotoAttribute::getId)
                .toList();

        List<AuditPhotos> auditListToDelete = allAuditPhotos.stream()
                .filter(it -> !photosIdsByUser.contains(it.getPhotoAttributeId()))
                .toList();

        log.info("audit items to delete {}", auditListToDelete.size());
        auditRepository.deleteAll(auditListToDelete);

        List<String> auditPhotoAttributeIds = allAuditPhotos.stream()
                .map(AuditPhotos::getPhotoAttributeId)
                .toList();

        List<PhotoAttribute> photosToDelete = byUserId.stream()
                .filter(it -> it.getStatus().equals(Status.SAVED_VECTOR_DB))
                .filter(it -> !auditPhotoAttributeIds.contains(it.getId()))
                .toList();

        vectorStore.delete(
                photosToDelete.stream()
                        .map(PhotoAttribute::getId)
                        .toList());

        log.info("photos items to delete {}", photosToDelete.size());
        photosRepository.deleteAll(photosToDelete);
    }
}
