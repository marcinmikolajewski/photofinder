package eu.mm.software.photofinder.photosattribute.infrastructure.mongo;

import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Repository
public interface PhotoAttributeSpringDataRepository extends MongoRepository<PhotoAttribute, String> {

    List<PhotoAttribute> findByUserId(String userId);

    @Aggregation(pipeline = {
            "{ '$match': { 'userId': ?0 } }",
            "{ '$group': { '_id': { 'filename': '$filename', 'path': '$path', 'storage': '$storage' }, 'ids': { '$push': '$_id' }, 'count': { '$sum': 1 } } }",
            "{ '$match': { 'count': { '$gt': 1 } } }",
            "{ '$project': { '_id': 0, 'duplicateIds': { '$slice': ['$ids', 1, { '$subtract': ['$count', 1] }] } } }"
    })
    List<ObjectId> findDuplicatePhotoIds(String userId);

    List<PhotoAttribute> findAllByProviderAndUserId(String provider, String userId);

    Long countAllByStatusAndUserId(Status status, String userId);

    Page<PhotoAttribute> findAllByUserId(String userId, Pageable pageable);

    List<PhotoAttribute> findAllByStatusAndUserId(Status status, String loggedUserId);

    @Query("{ 'userId': ?0, 'status': { '$nin': ?1 }, 'photoDescription': { '$exists': true, '$ne': '' } }")
    Stream<PhotoAttribute> findEligibleForEmbedding(String userId, Collection<String> excludedStatuses);
}
