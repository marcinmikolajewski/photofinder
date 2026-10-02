package eu.mm.software.photofinder.photosattribute.infrastructure.query;

import eu.mm.software.photofinder.photosattribute.application.query.VectorQuery;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorQueryImpl implements VectorQuery {

    public static final String USER_ID = "userId";
    public static final String PATH = "path";
    public static final String FILENAME = "filename";
    public static final String PROVIDER = "provider";
    private static final String MODEL = "model";
    public static final String STORAGE = "storage";

    private final VectorStore vectorStore;
    private final UserQuery userQuery;

    @Override
    public List<String> search(String prompt,
                               String provider,
                               String model,
                               String path,
                               Integer limit) {

        String loggedUserId = userQuery.findLoggedUserId();
        Filter.Expression expression = buildFilterExpression(loggedUserId, provider, model, path);

        SearchRequest request = new SearchRequest.Builder()
                .query(prompt)
                .topK(limit)
                .filterExpression(expression)
                .similarityThreshold(0.3)
                .build();

        log.info("Search vectorStore");
        List<Document> docs = vectorStore.similaritySearch(request);
        return Objects.requireNonNull(docs).stream()
                .map(this::formatDocument)
                .toList();
    }

    private String formatDocument(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();

        return String.format("%s Path: %s/%s Provider: %s model: %s storage: %s userId: %s",
                doc.getText(),
                getMetadataValue(metadata, PATH),
                getMetadataValue(metadata, FILENAME),
                getMetadataValue(metadata, PROVIDER),
                getMetadataValue(metadata, MODEL),
                getMetadataValue(metadata, STORAGE),
                getMetadataValue(metadata, "userId"));
    }

    private String getMetadataValue(Map<String, Object> metadata, String key) {
        return Optional.ofNullable(metadata.get(key))
                .map(Object::toString)
                .orElse("");
    }

    private Filter.Expression buildFilterExpression(String userId, String provider, String model, String path) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        FilterExpressionBuilder.Op expression = builder.eq(USER_ID, userId);

        if (StringUtils.isNotBlank(provider)) {
            expression = builder.and(expression, builder.eq(PROVIDER, provider));
        }

        if (StringUtils.isNotBlank(model)) {
            expression = builder.and(expression, builder.eq(MODEL, model));
        }

        if (StringUtils.isNotBlank(model)) {
            expression = builder.and(expression, builder.eq(PATH, path));
        }

        return expression.build();
    }
}
