package eu.mm.software.photofinder.photosattribute.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Base64;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiBodyDto {

    public static final String PREFIX = "data:image/jpeg;base64,";

    private String model;
    private List<OpenAiBodyDto.Messages> messages;
    private double temperature;

    public static OpenAiBodyDto createBody(String model, String prompt, byte[] imageData, double temperature) {
        OpenAiBodyDto.Messages.Content contentText =
                new OpenAiBodyDto.Messages.Content(OpenAiBodyDto.Messages.Content.Type.TEXT.getName(),
                        prompt,
                        null);
        OpenAiBodyDto.Messages.Content contentImage =
                new OpenAiBodyDto.Messages.Content(OpenAiBodyDto.Messages.Content.Type.IMAGE_URL.getName(),
                        null,
                        new OpenAiBodyDto.Messages.Content.ImageUrl(
                                PREFIX + Base64.getEncoder().encodeToString(imageData)));

        OpenAiBodyDto.Messages messages = new OpenAiBodyDto.Messages("user", List.of(contentText, contentImage));

        return new OpenAiBodyDto(model, List.of(messages), temperature);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Messages {
        private String role;
        private List<OpenAiBodyDto.Messages.Content> content;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Content {
            private String type;
            private String text;
            private OpenAiBodyDto.Messages.Content.ImageUrl image_url;

            @Getter
            @Setter
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class ImageUrl {
                private String url;
            }

            @Getter
            public enum Type {
                TEXT("text"),
                IMAGE_URL("image_url");

                final String name;

                Type(String name) {
                    this.name = name;
                }
            }
        }
    }
}
