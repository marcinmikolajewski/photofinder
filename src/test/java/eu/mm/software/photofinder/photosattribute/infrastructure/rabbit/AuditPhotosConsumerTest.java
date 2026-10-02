package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import eu.mm.software.photofinder.photosattribute.domain.AuditPhotoRepository;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.domain.event.PhotoAuditedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuditPhotosConsumerTest {

    private AuditPhotoRepository auditPhotoRepository;
    private AuditConsumer consumer;

    @BeforeEach
    void setUp() {
        auditPhotoRepository = mock(AuditPhotoRepository.class);
        consumer = new AuditConsumer(auditPhotoRepository);
    }

    @Test
    void onPhotoAudited_savesAuditWithAllFields() {
        // given
        PhotoAuditedEvent event = new PhotoAuditedEvent(
                "photo-1", "user-1", "OPENAI", "gpt-4o", 1500L, 250L, "DESCRIBED",
                "PC-Salon | Intel Core i7-8700K");

        // when
        consumer.onPhotoAudited(event);

        // then
        ArgumentCaptor<AuditPhotos> captor = ArgumentCaptor.forClass(AuditPhotos.class);
        verify(auditPhotoRepository).save(captor.capture());

        AuditPhotos saved = captor.getValue();
        assertThat(saved.getPhotoAttributeId()).isEqualTo("photo-1");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getProvider()).isEqualTo("OPENAI");
        assertThat(saved.getModel()).isEqualTo("gpt-4o");
        assertThat(saved.getTotalTokens()).isEqualTo(250L);
        assertThat(saved.getProcessingTimeMs()).isEqualTo(1500L);
        assertThat(saved.getStatus()).isEqualTo("DESCRIBED");
        assertThat(saved.getWorkerNode()).isEqualTo("PC-Salon | Intel Core i7-8700K");
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    @Test
    void onPhotoAudited_savesAuditWhenStatusIsError() {
        // given
        PhotoAuditedEvent event = new PhotoAuditedEvent(
                "photo-2", "user-1", "GEMINI", null, 800L, 0L, "ERROR", null);

        // when
        consumer.onPhotoAudited(event);

        // then
        ArgumentCaptor<AuditPhotos> captor = ArgumentCaptor.forClass(AuditPhotos.class);
        verify(auditPhotoRepository).save(captor.capture());

        AuditPhotos saved = captor.getValue();
        assertThat(saved.getPhotoAttributeId()).isEqualTo("photo-2");
        assertThat(saved.getStatus()).isEqualTo("ERROR");
        assertThat(saved.getProvider()).isEqualTo("GEMINI");
        assertThat(saved.getWorkerNode()).isNull();
    }

    @Test
    void onPhotoAudited_propagatesException_whenRepositoryFails() {
        // given
        PhotoAuditedEvent event = new PhotoAuditedEvent(
                "photo-3", "user-1", "OPENAI", "gpt-4o", 1000L, 100L, "DESCRIBED", "node-1");
        doThrow(new RuntimeException("MongoDB unavailable")).when(auditPhotoRepository).save(any());

        // when / then — wyjątek musi propagować, żeby RabbitMQ factory zrobiło retry / DLQ
        assertThatThrownBy(() -> consumer.onPhotoAudited(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MongoDB unavailable");
    }
}
