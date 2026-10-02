package eu.mm.software.photofinder.photosattribute.infrastructure.vectordb;

import com.google.common.collect.Lists;
import com.mongodb.client.result.UpdateResult;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.VectorDBRepository;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
class VectorDBRepositoryImpl implements VectorDBRepository {

    private final PhotoAttributeSpringDataRepository photoAttributeSpringDataRepository;
    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
            512,
            100,
            50,
            50,
            true,
            List.of('.', '!', '?', ',', ';', ':'));
    private final BatchingStrategy batchingStrategy;
    private final MongoTemplate mongoTemplate;

    private final UserQuery userQuery;
    private final RabbitTemplate rabbitTemplate;
    private final VectorDb vectorDb;


    @Override
    @Async
    public void sendEmbeddedToQueue(Boolean overwrite) {

        List<Document> documents = getDocuments(overwrite);

        List<List<Document>> splitList = batchingStrategy.batch(tokenTextSplitter.apply(documents));

        log.info("Split on {} lists", splitList.size());

        if (overwrite) {
            setAllDocumentAsDescribe();
        }

        splitList.forEach(splitPartDocuments -> rabbitTemplate.convertAndSend("ai.exchange",
                "ai.describe.embedded",
                splitPartDocuments));
        log.info("send embedded to que vectorDb: 100%");
    }

    @Override
    public void deleteAllByUserId(String userId) {
        log.info("Deleting vector DB entries for userId: {}", userId);

        vectorDb.deleteByUserId(userId);

        Criteria criteria = Criteria.where("userId").is(userId)
                .and("status").is(Status.SAVED_VECTOR_DB);

        Query query = new Query(criteria);
        Update update = new Update().set("status", Status.DESCRIBED);

        UpdateResult result = mongoTemplate.updateMulti(query, update, PhotoAttribute.class);
        log.info("Reset vector status for {} documents, userId: {}",
                result.getModifiedCount(), userId);
    }

    private List<Document> getDocuments(Boolean overwrite) {

        String loggedUserId = userQuery.findLoggedUserId();

        List<String> excludedStatuses = overwrite
                ? List.of(Status.ERROR.name(), Status.SEND_TO_QUEUE.name())
                : List.of(Status.ERROR.name(), Status.SEND_TO_QUEUE.name(), Status.SAVED_VECTOR_DB.name());

        try (Stream<PhotoAttribute> stream = photoAttributeSpringDataRepository.findEligibleForEmbedding(loggedUserId, excludedStatuses)) {
            return stream
                    .map(this::createDocument)
                    .toList();
        }
    }

    @Override
    public void sendEmbeddedToQueue(PhotoAttribute photoAttribute) {

        rabbitTemplate.convertAndSend("ai.exchange",
                "ai.describe.embedded",
                Lists.newArrayList(createDocument(photoAttribute)));
    }

    private Document createDocument(PhotoAttribute photoAttribute) {
        var metadata = new java.util.HashMap<String, Object>();
        metadata.put("id", photoAttribute.getId());
        metadata.put("userId", photoAttribute.getUserId());
        metadata.put("filename", photoAttribute.getFilename());
        metadata.put("path", photoAttribute.getPath());
        metadata.put("model", photoAttribute.getModel());
        metadata.put("provider", photoAttribute.getProvider());
        metadata.put("storage", photoAttribute.getStorage());

        if (photoAttribute.getCamera() != null)       metadata.put("camera", photoAttribute.getCamera());
        if (photoAttribute.getLens() != null)         metadata.put("lens", photoAttribute.getLens());
        if (photoAttribute.getFocalLength() != null)  metadata.put("focalLength", photoAttribute.getFocalLength());
        if (photoAttribute.getAperture() != null)     metadata.put("aperture", photoAttribute.getAperture());
        if (photoAttribute.getIso() != null)          metadata.put("iso", photoAttribute.getIso());

        return new Document(photoAttribute.getPhotoDescription(), metadata);
    }

    private void setAllDocumentAsDescribe() {

        Criteria criteriaUserId = new Criteria().andOperator(Criteria.where("userId").is(userQuery.findLoggedUserId()));
        Criteria criteriaStatusERROR = new Criteria().andOperator(Criteria.where("status").ne(Status.ERROR.name()));
        Criteria criteriaStatusSEND_TO_QUEUE = new Criteria().andOperator(Criteria.where("status").ne(Status.SEND_TO_QUEUE.name()));

        Criteria criteriaAll = new Criteria().andOperator(criteriaUserId, criteriaStatusERROR, criteriaStatusSEND_TO_QUEUE);
        Query query = new Query(criteriaAll);

        Update update = new Update()
                .set("status", Status.DESCRIBED);
        UpdateResult result = mongoTemplate.updateMulti(query, update, PhotoAttribute.class);

        log.info("Updated documents count: {}", result.getModifiedCount());
    }
}
