package eu.mm.software.photofinder.worker;

import eu.mm.software.photofinder.worker.event.PhotoJobMessage;
import eu.mm.software.photofinder.worker.event.PhotoProcessedEvent;
import eu.mm.software.photofinder.worker.event.Status;
import eu.mm.software.photofinder.worker.storage.WorkerMinioStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaWorkerConsumer {

    private static final String PROMPT_BASE = "Describe this image in english";
    private static final double TEMPERATURE = 0.3;
    private static final String METADATA_TOTAL_DURATION = "total-duration";

    private final OllamaChatModel chatModel;
    private final WorkerMinioStorage minioStorage;
    private final RabbitTemplate rabbitTemplate;
    private final WorkerNodeInfo workerNodeInfo;

    @Value("${ollama.model.name:llava:13b}")
    private String model;

    @RabbitListener(queues = "ai.ollama.queue", containerFactory = "workerListenerFactory")
    public void receive(PhotoJobMessage message) {
        log.info("Processing photo: {} on node: {}", message.photoId(), workerNodeInfo.getNodeLabel());
        try {
            // Spójnie z główną apką: w razie błędu rzucamy wyjątek → 3 retry → DLQ →
            // DeadLetterConsumer oznacza ERROR i audytuje. Obraz kasujemy DOPIERO po sukcesie,
            // żeby próby ponowne (i ścieżka DLQ) miały do czego wrócić.
            byte[] imageData = minioStorage.load(message.imageRef());

            String prompt = buildPrompt(message.exifContext());
            ChatResponse response = callOllama(imageData, prompt);
            long inferenceMs = extractInferenceMs(response);
            long totalTokens = extractTotalTokens(response);
            String description = response.getResults().stream()
                    .findFirst()
                    .map(g -> g.getOutput().getText())
                    .orElse("");
            log.info("Described photo: {} in {} ms ({} tokens)", message.photoId(), inferenceMs, totalTokens);

            PhotoProcessedEvent result = new PhotoProcessedEvent(
                    message.photoId(), message.userId(), description, model,
                    Status.DESCRIBED, "OLLAMA", inferenceMs, totalTokens, workerNodeInfo.getNodeLabel());

            minioStorage.delete(message.imageRef());
            rabbitTemplate.convertAndSend("ai.exchange", "ai.event.photo.processed", result);
        } catch (RuntimeException e) {
            // Log z kontekstem, potem rethrow — niech zadziała retry → DLQ.
            log.error("Failed to describe photo: {} (imageRef: {}) on node: {}",
                    message.photoId(), message.imageRef(), workerNodeInfo.getNodeLabel(), e);
            throw e;
        }
    }

    private String buildPrompt(String exifContext) {
        if (exifContext == null || exifContext.isBlank()) return PROMPT_BASE;
        return PROMPT_BASE + ". Camera settings: " + exifContext + ".";
    }

    private ChatResponse callOllama(byte[] imageData, String prompt) {
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageData));
        var userMessage = UserMessage.builder()
                .text(prompt)
                .media(List.of(media))
                .build();

        return chatModel.call(
                new Prompt(List.of(userMessage), OllamaChatOptions.builder()
                        .model(model)
                        .temperature(TEMPERATURE)
                        .build()));
    }

    private long extractInferenceMs(ChatResponse response) {
        Object raw = response.getMetadata().get(METADATA_TOTAL_DURATION);
        if (raw instanceof Duration d) {
            return d.toMillis();
        }
        return 0L;
    }

    private long extractTotalTokens(ChatResponse response) {
        var usage = response.getMetadata().getUsage();
        if (usage == null) {
            return 0L;
        }
        return usage.getTotalTokens();
    }
}
