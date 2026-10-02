package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.common.WorkerNodeInfo;
import eu.mm.software.photofinder.common.metrics.PhotoMetrics;
import eu.mm.software.photofinder.photosattribute.application.query.AIProcessor;
import eu.mm.software.photofinder.photosattribute.application.query.ResponseAiDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.domain.ImageNotFoundException;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.TemporaryImageStorage;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoJobMessage;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoProcessedEvent;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotosConsumer {

    private final AIProcessor aiProcessor;
    private final TemporaryImageStorage temporaryImageStorage;
    private final DomainEventPublisher eventPublisher;
    private final PhotoMetrics photoMetrics;
    private final WorkerNodeInfo workerNodeInfo;

    @RabbitListener(queues = "ai.ollama.queue", containerFactory = "slowListenerFactory")
    public void receiveOllama(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.groq.queue", containerFactory = "slowListenerFactory")
    public void receiveGroq(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.nvidia.queue", containerFactory = "slowListenerFactory")
    public void receiveNvidia(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.together.queue", containerFactory = "slowListenerFactory")
    public void receiveTogether(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.mistral.queue", containerFactory = "slowListenerFactory")
    public void receiveMistral(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.openai.queue", containerFactory = "slowListenerFactory")
    public void receiveOpenAi(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.hyperbolic.queue", containerFactory = "slowListenerFactory")
    public void receiveHyperbolic(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.gemini.queue", containerFactory = "slowListenerFactory")
    public void receiveGemini(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.cohere.queue", containerFactory = "slowListenerFactory")
    public void receiveCohere(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.openrouter.queue", containerFactory = "slowListenerFactory")
    public void receiveOpenRouter(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.cloudflare.queue", containerFactory = "slowListenerFactory")
    public void receiveCloudFlare(PhotoJobMessage message) { receive(message); }

    @RabbitListener(queues = "ai.anthropic.queue", containerFactory = "slowListenerFactory")
    public void receiveAnthropic(PhotoJobMessage message) { receive(message); }

    public void receive(PhotoJobMessage message) {
        try {
            process(message);
        } catch (ImageNotFoundException e) {
            // Błąd liczony raz, finalnie w DeadLetterConsumer — nie per próba retry
            log.warn("Temporary image missing for photo: {}, imageRef: {} — routing to DLQ",
                    message.photoId(), message.imageRef());
            throw new AmqpRejectAndDontRequeueException(e);
        } catch (RuntimeException e) {
            // Błąd liczony raz, finalnie w DeadLetterConsumer — nie per próba retry
            log.warn("Failed to process photo: {}, imageRef: {}",
                    message.photoId(), message.imageRef(), e);
            throw e;
        }
    }

    public void process(PhotoJobMessage message) {
        log.info("Processing photo: {} with provider: {}",
                message.photoId(), message.provider());

        byte[] imageData = temporaryImageStorage.load(message.imageRef());

        long startTime = System.currentTimeMillis();
        ResponseAiDto response = aiProcessor.describePhoto(
                imageData,
                AiProvider.valueOf(message.provider()));
        long elapsed = System.currentTimeMillis() - startTime;

        temporaryImageStorage.delete(message.imageRef());

        log.info("Described photo: {} in {} ms by {} / {} ({} tokens)",
                message.photoId(), elapsed, message.provider(), response.model(), response.totalToken());

        if (response.status() == Status.DESCRIBED) {
            photoMetrics.photoDescribed(elapsed, message.provider());
        } else {
            photoMetrics.photoDescribeError(message.provider());
        }

        eventPublisher.publish(new PhotoProcessedEvent(
                message.photoId(),
                message.userId(),
                response.response(),
                response.model(),
                response.status(),
                message.provider(),
                elapsed,
                response.totalToken(),
                workerNodeInfo.getNodeLabel()
        ));
    }
}
