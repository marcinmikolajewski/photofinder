package eu.mm.software.photofinder.photosattribute.infrastructure.mongo;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AuditPhotoSpringDataRepository extends MongoRepository<AuditPhotos, String> {

    Set<AuditPhotos> findAllByUserId(String userId);

    Set<AuditPhotos> findAllByUserIdAndProvider(String userId, String provider);

    Set<AuditPhotos> findAllByUserIdAndStatus(String userId, String status);

    long countByUserId(String userId);

    void deleteAllByUserId(String userId);
}
