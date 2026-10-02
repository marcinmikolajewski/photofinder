package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.TemporaryImageStorage;
import eu.mm.software.photofinder.photosattribute.domain.VectorDBRepository;
import eu.mm.software.photofinder.user.domain.event.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class UserDeletedEventConsumerTest {

    private PhotoAttributeRepository photoAttributeRepository;
    private AuditPhotoRepository auditPhotoRepository;
    private VectorDBRepository vectorDBRepository;
    private TemporaryImageStorage temporaryImageStorage;

    private UserDeletedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        photoAttributeRepository = mock(PhotoAttributeRepository.class);
        auditPhotoRepository = mock(AuditPhotoRepository.class);
        vectorDBRepository = mock(VectorDBRepository.class);
        temporaryImageStorage = mock(TemporaryImageStorage.class);
        consumer = new UserDeletedEventConsumer(
                photoAttributeRepository, auditPhotoRepository, vectorDBRepository, temporaryImageStorage);
    }

    @Test
    void onUserDeleted_deletesTemporaryImagesForUser() {
        // given
        UserDeletedEvent event = new UserDeletedEvent("user-123");

        // when
        consumer.onUserDeleted(event);

        // then
        verify(temporaryImageStorage).deleteAllForUser("user-123");
    }

    @Test
    void onUserDeleted_deletesAuditEntriesForUser() {
        // given
        UserDeletedEvent event = new UserDeletedEvent("user-123");

        // when
        consumer.onUserDeleted(event);

        // then
        verify(auditPhotoRepository).deleteAllByUserId("user-123");
    }

    @Test
    void onUserDeleted_deletesVectorDbEntriesForUser() {
        // given
        UserDeletedEvent event = new UserDeletedEvent("user-123");

        // when
        consumer.onUserDeleted(event);

        // then
        verify(vectorDBRepository).deleteAllByUserId("user-123");
    }

    @Test
    void onUserDeleted_deletesPhotoAttributesForUser() {
        // given
        UserDeletedEvent event = new UserDeletedEvent("user-123");

        // when
        consumer.onUserDeleted(event);

        // then
        verify(photoAttributeRepository).deleteAllByUserId("user-123");
    }

    @Test
    void onUserDeleted_deletesAllFourRepositories_forGivenUserId() {
        // given
        UserDeletedEvent event = new UserDeletedEvent("user-456");

        // when
        consumer.onUserDeleted(event);

        // then
        verify(temporaryImageStorage).deleteAllForUser("user-456");
        verify(auditPhotoRepository).deleteAllByUserId("user-456");
        verify(vectorDBRepository).deleteAllByUserId("user-456");
        verify(photoAttributeRepository).deleteAllByUserId("user-456");
        verifyNoMoreInteractions(temporaryImageStorage, auditPhotoRepository, vectorDBRepository, photoAttributeRepository);
    }
}
