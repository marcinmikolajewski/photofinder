package eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAiBodyDto {
    @JsonProperty("contents")
    private List<Content> contents;


    @Getter
    @Setter
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
    }

    @Getter
    @Setter
    public static class Part {
        @JsonProperty("text")
        private String text;

        @JsonProperty("inline_data")
        private InlineData inlineData;
    }

    @Getter
    @Setter
    public static class InlineData {
        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("data")
        private String data;
    }
}

