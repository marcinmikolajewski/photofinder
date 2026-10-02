package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoDescribedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoDescribedEventConsumer {

    private final PhotoAttributeRepository photoRepository;

    @RabbitListener(queues = "app.vector.save.queue", containerFactory = "slowListenerFactory")
    public void onPhotoDescribed(PhotoDescribedEvent event) {
        log.info("Handling PhotoDescribedEvent for photoId: {}", event.photoId());
        photoRepository.saveVector(event.photoId());
    }
}