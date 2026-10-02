package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoDescribedEvent;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoPersistenceConsumer {

    private final PhotoAttributeRepository photoRepository;
    private final DomainEventPublisher eventPublisher;

    @RabbitListener(queues = "app.photo.save.queue", containerFactory = "persistenceListenerFactory")
    public void onPhotoProcessed(PhotoProcessedEvent event) {
        log.info("Persisting photo: {}", event.photoId());

        photoRepository.updateAfterDescribe(
                event.photoId(),
                event.description(),
                event.model(),
                event.status(),
                event.provider());

        eventPublisher.publish(new PhotoAuditedEvent(
                event.photoId(),
                event.userId(),
                event.provider(),
                event.model(),
                event.processingTimeMs(),
                event.totalTokens(),
                event.status().name(),
                event.workerNode()
        ));

        if (event.status() == Status.DESCRIBED) {
            eventPublisher.publish(new PhotoDescribedEvent(
                    event.photoId(),
                    event.userId(),
                    event.provider()
            ));
        }
    }
}
