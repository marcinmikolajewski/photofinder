package eu.mm.software.photofinder.photosattribute.infrastructure.vectordb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDb {
    private final VectorStore vectorStore;

    public void addVector(List<Document> splitPartDocuments) {

        vectorStore.add(splitPartDocuments);
    }

    public void deleteByUserId(String userId) {
        vectorStore.delete(
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("userId"),
                        new Filter.Value(userId)
                )
        );
        log.info("Deleted vectors for userId: {}", userId);
    }
}
