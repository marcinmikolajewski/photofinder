package eu.mm.software.photofinder.common.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private MongoTemplate mongoTemplate;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        service = new RefreshTokenService(mongoTemplate);
    }

    @Test
    void isCurrent_returnsFalse_forNullJti_withoutHittingMongo() {
        assertThat(service.isCurrent("user@example.com", null)).isFalse();
        assertThat(service.isCurrent("user@example.com", "  ")).isFalse();
        verify(mongoTemplate, never()).exists(any(Query.class), anyString());
    }

    @Test
    void isCurrent_delegatesToMongoExists() {
        when(mongoTemplate.exists(any(Query.class), eq("refresh_tokens"))).thenReturn(true);

        assertThat(service.isCurrent("user@example.com", "jti-123")).isTrue();
        verify(mongoTemplate).exists(any(Query.class), eq("refresh_tokens"));
    }

    @Test
    void rotate_upsertsCurrentJti() {
        service.rotate("user@example.com", "jti-123");
        verify(mongoTemplate).upsert(any(Query.class), any(), eq("refresh_tokens"));
    }

    @Test
    void revoke_removesRecord() {
        service.revoke("user@example.com");
        verify(mongoTemplate).remove(any(Query.class), eq("refresh_tokens"));
    }
}
