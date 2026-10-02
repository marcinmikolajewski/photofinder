package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.infrastructure.vectordb.VectorDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddedProcessingService {
    private final VectorDb vectorDb;
    private final MongoTemplate mongoTemplate;
    private final PhotoMetrics photoMetrics;

    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void processMessage(List<DocumentMessage> messages) {

        List<Document> documents = messages.stream()
                .map(doc -> new Document(doc.id(), doc.text(), doc.metadata()))
                .toList();

        vectorDb.addVector(documents);

        log.info("Thread: {} save: {}", Thread.currentThread().getName(), documents);
        updateDocuments(getIds(documents), Status.SAVED_VECTOR_DB);
        photoMetrics.photoSavedToVector();
    }

    @Recover
    public void recover(Exception e, List<DocumentMessage> messages) {

        Set<String> ids = messages.stream()
                .map(DocumentMessage::id)
                .collect(Collectors.toSet());

        updateDocuments(ids, Status.ERROR_VECTOR_DB);
        photoMetrics.photoVectorError();    // ← dodane


        log.error("Message processing failed permanently: {}", messages, e);
    }

    private void updateDocuments(Set<String> ids, Status status) {

        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(ids));
        Update update = new Update();
        update.set("status", status);

        mongoTemplate.updateMulti(query, update, PhotoAttribute.class);
    }

    private static Set<String> getIds(List<Document> splitPartDocuments) {
        return splitPartDocuments.stream()
                .map(it -> (String) it.getMetadata().get("id"))
                .collect(Collectors.toSet());
    }
}
