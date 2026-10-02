package eu.mm.software.photofinder.photosattribute.infrastructure.repository;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.AuditPhotoSpringDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPhotoRepositoryImpl implements AuditPhotoRepository {

    private final AuditPhotoSpringDataRepository auditPhotoSpringDataRepository;

    @Override
    public void save(AuditPhotos auditPhotos) {

        auditPhotoSpringDataRepository.save(auditPhotos);
    }

    @Override
    public void deleteAllByUserId(String userId) {
        log.info("Deleting audit entries for userId: {}", userId);
        long count = auditPhotoSpringDataRepository.countByUserId(userId);
        auditPhotoSpringDataRepository.deleteAllByUserId(userId);
        log.info("Deleted {} audit entries for userId: {}", count, userId);
    }
}
