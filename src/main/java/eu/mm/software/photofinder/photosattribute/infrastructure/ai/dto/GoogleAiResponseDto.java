package eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleAiResponseDto {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    @JsonProperty("modelVersion")
    private String modelVersion;

    @JsonProperty("responseId")
    private String responseId;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter
    @Getter
    public static class Candidate {
        @JsonProperty("content")
        private Content content;

        @JsonProperty("finishReason")
        private String finishReason;

        @JsonProperty("avgLogprobs")
        private Double avgLogprobs;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter
    @Getter
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter
    @Getter
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter
    @Getter
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;

        @JsonProperty("promptTokensDetails")
        private List<TokenDetail> promptTokensDetails;

        @JsonProperty("candidatesTokensDetails")
        private List<TokenDetail> candidatesTokensDetails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter
    @Getter
    public static class TokenDetail {
        @JsonProperty("modality")
        private String modality;

        @JsonProperty("tokenCount")
        private Integer tokenCount;
    }
}

