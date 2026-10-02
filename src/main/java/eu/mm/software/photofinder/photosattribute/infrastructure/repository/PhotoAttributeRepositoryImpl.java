package eu.mm.software.photofinder.photosattribute.infrastructure.repository;

import com.mongodb.client.result.UpdateResult;
import eu.mm.software.photofinder.photosattribute.domain.PhotoParamsDto;
import eu.mm.software.photofinder.photosattribute.domain.*;
import org.springframework.dao.DuplicateKeyException;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class PhotoAttributeRepositoryImpl implements PhotoAttributeRepository {

    private final PhotoAttributeSpringDataRepository repository;
    private final VectorDBRepository vectorDBRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public String save(PhotoParamsDto form) {
        try {
            return repository.save(createPhotoAttribute(form)).getId();
        } catch (DuplicateKeyException e) {
            throw new DuplicatePhotoException(form.getPath(), form.getFileName());
        }
    }

    @Override
    public void saveVector(String photoId) {

        repository.findById(photoId)
                .ifPresentOrElse(
                        vectorDBRepository::sendEmbeddedToQueue,
                        () -> log.warn("PhotoAttribute not found for id: {}", photoId)
                );
    }

    @Override
    public void updateAfterDescribe(String photoId,
                                    String description,
                                    String model,
                                    Status status,
                                    String provider) {

        Query query = new Query(Criteria.where("id").is(photoId));

        Update update = new Update()
                .set("photoDescription", description)
                .set("model", model)
                .set("status", status)
                .set("provider", provider);

        UpdateResult result = mongoTemplate.updateFirst(query, update, PhotoAttribute.class);

        if (result.getMatchedCount() == 0) {
            log.warn("PhotoAttribute not found for id: {}", photoId);
        } else {
            log.info("Updated PhotoAttribute: {} status: {}", photoId, status);
        }
    }

    @Override
    public void updateStatusToError(String photoId) {
        Query query = new Query(Criteria.where("id").is(photoId)
                .and("status").nin(Status.DESCRIBED, Status.SAVED_VECTOR_DB));
        UpdateResult result = mongoTemplate.updateFirst(query, new Update().set("status", Status.ERROR), PhotoAttribute.class);
        if (result.getModifiedCount() > 0) {
            log.warn("Marked photo {} as ERROR after DLQ", photoId);
        }
    }

    @Override
    public void deleteAllByUserId(String userId) {
        log.info("Deleting photo attributes for userId: {}", userId);

        Query query = new Query(Criteria.where("userId").is(userId));

        long count = mongoTemplate.count(query, PhotoAttribute.class);
        mongoTemplate.remove(query, PhotoAttribute.class);

        log.info("Deleted {} photo attributes for userId: {}", count, userId);
    }

    private static PhotoAttribute createPhotoAttribute(PhotoParamsDto form) {
        return new PhotoAttribute(form.getId(),
                form.getFileName(),
                form.getPath(),
                form.getPhotoDescription(),
                form.getProvider(),
                form.getModel(),
                form.getStatus(),
                form.getUserId(),
                Storage.valueOf(form.getStorage()),
                form.getCamera(),
                form.getLens(),
                form.getFocalLength(),
                form.getAperture(),
                form.getShutterSpeed(),
                form.getIso());
    }
}
