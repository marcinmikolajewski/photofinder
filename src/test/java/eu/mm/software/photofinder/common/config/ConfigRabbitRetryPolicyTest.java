package eu.mm.software.photofinder.common.config;

import eu.mm.software.photofinder.photosattribute.domain.ImageNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Weryfikuje politykę retry ścieżki opisu (slowListenerFactory):
 * brakująca miniatura NIE jest ponawiana (od razu DLQ), reszta tak.
 */
class ConfigRabbitRetryPolicyTest {

    private final SimpleRetryPolicy policy = ConfigRabbit.slowListenerRetryPolicy();

    @Test
    void doesNotRetry_imageNotFoundException() {
        assertThat(canRetry(new ImageNotFoundException("ref"))).isFalse();
    }

    @Test
    void doesNotRetry_amqpRejectAndDontRequeueException() {
        assertThat(canRetry(new AmqpRejectAndDontRequeueException("reject"))).isFalse();
    }

    @Test
    void doesNotRetry_amqpRejectWrappingImageNotFound_viaCauseTraversal() {
        // receive() opakowuje ImageNotFoundException w AmqpRejectAndDontRequeueException
        assertThat(canRetry(new AmqpRejectAndDontRequeueException(new ImageNotFoundException("ref")))).isFalse();
    }

    @Test
    void retries_genericRuntimeException() {
        assertThat(canRetry(new RuntimeException("transient 500"))).isTrue();
    }

    private boolean canRetry(Throwable throwable) {
        RetryContext context = policy.open(null);
        policy.registerThrowable(context, throwable);
        return policy.canRetry(context);
    }
}
