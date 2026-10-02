package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

@Component
public class GeminiRateLimiter {
    private final RateLimiter rateLimiter = RateLimiter.create(0.25);

    public void acquire() {
        rateLimiter.acquire(); // blokuje do czasu dostępnego tokena
    }
}
