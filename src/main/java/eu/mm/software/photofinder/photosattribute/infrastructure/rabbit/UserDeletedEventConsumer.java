package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.TemporaryImageStorage;
import eu.mm.software.photofinder.photosattribute.domain.VectorDBRepository;
import eu.mm.software.photofinder.user.domain.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedEventConsumer {

    private final PhotoAttributeRepository photoAttributeRepository;
    private final AuditPhotoRepository auditPhotoRepository;
    private final VectorDBRepository vectorDBRepository;
    private final TemporaryImageStorage temporaryImageStorage;

    @RabbitListener(queues = "ai.user.deleted.queue",
            containerFactory = "rabbitMultiCoreListenerContainerFactory")
    public void onUserDeleted(UserDeletedEvent event) {
        String userId = event.userId();
        log.info("Cleaning up data for deleted user: {}", userId);

        temporaryImageStorage.deleteAllForUser(userId);
        log.info("Deleted tmp images from MinIO for user: {}", userId);

        auditPhotoRepository.deleteAllByUserId(userId);
        log.info("Deleted audit entries for user: {}", userId);

        vectorDBRepository.deleteAllByUserId(userId);
        log.info("Deleted vector DB entries for user: {}", userId);

        photoAttributeRepository.deleteAllByUserId(userId);
        log.info("Deleted photo attributes for user: {}", userId);
    }
}