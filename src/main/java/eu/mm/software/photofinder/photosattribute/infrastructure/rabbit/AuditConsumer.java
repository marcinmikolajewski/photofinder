package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditConsumer {

   private final AuditPhotoRepository auditPhotoRepository;

    @RabbitListener(queues = "app.audit.queue",
            containerFactory = "rabbitMultiCoreListenerContainerFactory")
    public void onPhotoAudited(PhotoAuditedEvent event) {
        log.info("Saving audit for photoId: {}", event.photoId());

        AuditPhotos audit = new AuditPhotos();
        audit.setPhotoAttributeId(event.photoId());
        audit.setUserId(event.userId());
        audit.setProvider(event.provider());
        audit.setModel(event.model());
        audit.setProcessingTimeMs(event.processingTimeMs());
        audit.setTotalTokens(event.totalTokens());
        audit.setStatus(event.status());
        audit.setWorkerNode(event.workerNode());
        audit.setOccurredAt(Instant.now());

        auditPhotoRepository.save(audit);
    }
}