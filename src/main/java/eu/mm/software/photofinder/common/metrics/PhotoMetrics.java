package eu.mm.software.photofinder.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class PhotoMetrics {

    private final Counter photosQueued;
    private final Counter photosDescribed;
    private final Counter photosDescribeError;
    private final Counter photosSavedToVector;
    private final Counter photosVectorError;
    private final Timer aiProcessingTimer;

    public PhotoMetrics(MeterRegistry registry) {

        photosQueued = Counter.builder("photos.queued.total")
                .description("Total photos sent to AI queue")
                .register(registry);


        photosDescribed = Counter.builder("photos.described.total")
                .description("Total photos successfully described by AI")
                .register(registry);


        photosDescribeError = Counter.builder("photos.describe.errors.total")
                .description("Total AI description failures")
                .register(registry);


        photosSavedToVector = Counter.builder("photos.vector.saved.total")
                .description("Total photos saved to vector DB")
                .register(registry);


        photosVectorError = Counter.builder("photos.vector.errors.total")
                .description("Total vector DB save failures")
                .register(registry);


        aiProcessingTimer = Timer.builder("photos.ai.processing.seconds")
                .description("AI photo description processing time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void photoQueued() {
        photosQueued.increment();
    }

    public void photoDescribed(long elapsedMs, String provider) {
        photosDescribed.increment();
        aiProcessingTimer.record(Duration.ofMillis(elapsedMs));
    }

    public void photoDescribeError(String provider) {
        photosDescribeError.increment();
    }

    public void photoSavedToVector() {
        photosSavedToVector.increment();
    }

    public void photoVectorError() {
        photosVectorError.increment();
    }
}