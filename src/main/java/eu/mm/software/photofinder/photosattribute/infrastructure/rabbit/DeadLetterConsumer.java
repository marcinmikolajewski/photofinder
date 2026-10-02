package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.mm.software.photofinder.common.WorkerNodeInfo;
import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final PhotoAttributeRepository photoAttributeRepository;
    private final DomainEventPublisher eventPublisher;
    private final WorkerNodeInfo workerNodeInfo;
    private final ObjectMapper objectMapper;
    private final PhotoMetrics photoMetrics;

    @RabbitListener(queues = "dlq")
    public void onDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String originalQueue = getHeader(message, "x-first-death-queue");
        String reason = getHeader(message, "x-first-death-reason");

        log.error("DLQ message from queue={} reason={} body={}",
                originalQueue, reason, body.length() > 200 ? body.substring(0, 200) + "..." : body);

        if (isAiProviderQueue(originalQueue)) {
            handleAiQueueFailure(body, originalQueue);
        }
    }

    /**
     * Sprawdza, czy kolejka przewozi {@link PhotoJobMessage} — czyli czy jej środkowy
     * segment odpowiada providerowi z {@link AiProvider} (np. "ai.ollama.queue" → OLLAMA).
     *
     * <p>Wyklucza inne kolejki kierowane do DLQ, które tylko z nazwy przypominają
     * provider-owe (np. "ai.embedded.queue", "ai.user.deleted.queue"), a przewożą
     * zupełnie inne typy wiadomości.</p>
     */
    private boolean isAiProviderQueue(String queue) {
        if (!queue.startsWith("ai.") || !queue.endsWith(".queue")) {
            return false;
        }
        String provider = queue.substring("ai.".length(), queue.length() - ".queue".length());
        return Arrays.stream(AiProvider.values())
                .anyMatch(p -> p.name().equalsIgnoreCase(provider));
    }

    private void handleAiQueueFailure(String body, String originalQueue) {
        try {
            PhotoJobMessage job = objectMapper.readValue(body, PhotoJobMessage.class);
            photoAttributeRepository.updateStatusToError(job.photoId());
            photoMetrics.photoDescribeError(job.provider());
            eventPublisher.publish(new PhotoAuditedEvent(
                    job.photoId(),
                    job.userId(),
                    job.provider(),
                    null,
                    null,
                    null,
                    Status.ERROR.name(),
                    workerNodeInfo.getNodeLabel()
            ));
        } catch (Exception e) {
            log.warn("Could not handle DLQ message from queue={}: {}", originalQueue, e.getMessage());
        }
    }

    private String getHeader(Message message, String key) {
        Object val = message.getMessageProperties().getHeaders().get(key);
        return val != null ? val.toString() : "unknown";
    }
}
