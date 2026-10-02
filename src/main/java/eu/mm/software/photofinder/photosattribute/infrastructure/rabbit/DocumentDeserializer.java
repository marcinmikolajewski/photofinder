package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DocumentDeserializer extends JsonDeserializer<Document> {
    @Override
    public Document deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String content = node.get("text").asText();
        JsonNode metadataNode = node.get("metadata");
        Map<String,Object> metaData= new HashMap<>();

        metaData.put("path",metadataNode.get("path").asText());
        metaData.put("filename",metadataNode.get("filename").asText());
        metaData.put("provider",metadataNode.get("provider").asText());
        metaData.put("model",metadataNode.get("model").asText());
        metaData.put("id",metadataNode.get("id").asText());
        metaData.put("storage",metadataNode.get("storage").asText());
        metaData.put("userId",metadataNode.get("userId").asText());

        String id = node.get("id").asText();

        return Document.builder()
                .text(content)
                .metadata(metaData)
                .id(id)
                .build();
    }
}
