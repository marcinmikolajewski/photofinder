package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.PhotoAttributeRepository;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.event.DomainEventPublisher;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoDescribedEvent;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PhotoPersistenceConsumerTest {

    private PhotoAttributeRepository photoRepository;
    private DomainEventPublisher eventPublisher;

    private PhotoPersistenceConsumer consumer;

    @BeforeEach
    void setUp() {
        photoRepository = mock(PhotoAttributeRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        consumer = new PhotoPersistenceConsumer(photoRepository, eventPublisher);
    }

    @Test
    void onPhotoProcessed_updatesPhotoWithAllEventFields() {
        // given
        PhotoProcessedEvent event = new PhotoProcessedEvent(
                "photo-1", "user-1", "sunset over mountains", "gpt-4o",
                Status.DESCRIBED, "OPENAI", 1500L, 250L, "PC-Main | AMD Ryzen 5");

        // when
        consumer.onPhotoProcessed(event);

        // then
        verify(photoRepository).updateAfterDescribe(
                "photo-1", "sunset over mountains", "gpt-4o", Status.DESCRIBED, "OPENAI");
    }

    @Test
    void onPhotoProcessed_publishesBothAuditAndDescribedEvent_whenStatusIsDescribed() {
        // given
        PhotoProcessedEvent event = new PhotoProcessedEvent(
                "photo-1", "user-1", "description", "gpt-4o",
                Status.DESCRIBED, "OPENAI", 1500L, 250L, "PC-Main | AMD Ryzen 5");

        // when
        consumer.onPhotoProcessed(event);

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(any());

        verify(eventPublisher).publish(argThat(e -> e instanceof PhotoAuditedEvent a
                && "photo-1".equals(a.photoId())
                && "DESCRIBED".equals(a.status())
                && "PC-Main | AMD Ryzen 5".equals(a.workerNode())));

        verify(eventPublisher).publish(argThat(e -> e instanceof PhotoDescribedEvent d
                && "photo-1".equals(d.photoId())
                && "user-1".equals(d.userId())
                && "OPENAI".equals(d.provider())));
    }

    @Test
    void onPhotoProcessed_mapsAllFieldsOntoAuditEvent() {
        // given — pełny event ze wszystkimi polami
        PhotoProcessedEvent event = new PhotoProcessedEvent(
                "photo-9", "user-9", "a description", "claude-3-5-sonnet",
                Status.DESCRIBED, "ANTHROPIC", 4200L, 777L, "PC-GPU | RTX 4060 Ti");

        // when
        consumer.onPhotoProcessed(event);

        // then — KAŻDE pole audytu musi pochodzić z odpowiedniego pola źródłowego
        ArgumentCaptor<PhotoAuditedEvent> captor = ArgumentCaptor.forClass(PhotoAuditedEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());

        PhotoAuditedEvent audit = captor.getAllValues().stream()
                .filter(PhotoAuditedEvent.class::isInstance)
                .map(PhotoAuditedEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(audit.photoId()).isEqualTo("photo-9");
        assertThat(audit.userId()).isEqualTo("user-9");
        assertThat(audit.provider()).isEqualTo("ANTHROPIC");
        assertThat(audit.model()).isEqualTo("claude-3-5-sonnet");
        assertThat(audit.processingTimeMs()).isEqualTo(4200L);
        assertThat(audit.totalTokens()).isEqualTo(777L);
        assertThat(audit.status()).isEqualTo("DESCRIBED");
        assertThat(audit.workerNode()).isEqualTo("PC-GPU | RTX 4060 Ti");
    }

    @Test
    void onPhotoProcessed_publishesOnlyAuditEvent_whenStatusIsError() {
        // given
        PhotoProcessedEvent event = new PhotoProcessedEvent(
                "photo-2", "user-1", null, null,
                Status.ERROR, "GEMINI", 800L, 0L, "PC-Salon | Intel i5");

        // when
        consumer.onPhotoProcessed(event);

        // then
        verify(eventPublisher, times(1)).publish(any());
        verify(eventPublisher).publish(argThat(e -> e instanceof PhotoAuditedEvent a
                && "ERROR".equals(a.status())));
    }

    @Test
    void onPhotoProcessed_publishesOnlyAuditEvent_whenStatusIsSentToQueue() {
        // given
        PhotoProcessedEvent event = new PhotoProcessedEvent(
                "photo-3", "user-1", null, null,
                Status.SEND_TO_QUEUE, "OLLAMA", 0L, 0L, null);

        // when
        consumer.onPhotoProcessed(event);

        // then
        verify(eventPublisher, times(1)).publish(any());
        verify(eventPublisher).publish(argThat(e -> e instanceof PhotoAuditedEvent a
                && "SEND_TO_QUEUE".equals(a.status())));
    }
}
