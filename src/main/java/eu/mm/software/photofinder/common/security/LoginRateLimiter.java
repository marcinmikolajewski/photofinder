package eu.mm.software.photofinder.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosty, in-memory limiter prób logowania per klucz (zwykle IP) — fixed window.
 * Chroni {@code /authenticate} przed brute-force / credential stuffing.
 *
 * <p>Wystarczający dla pojedynczej instancji. Przy wielu instancjach / za load balancerem
 * potrzebny byłby wspólny store (np. Redis / bucket4j-redis).</p>
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMs;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${security.login.max-attempts:10}") int maxAttempts,
            @Value("${security.login.window-ms:60000}") long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /**
     * @return true jeśli próba mieści się w limicie; false gdy limit przekroczony.
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Counter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMs) {
                return new Counter(now); // nowe okno
            }
            existing.count++;
            return existing;
        });
        return counter.count <= maxAttempts;
    }

    private static final class Counter {
        final long windowStart;
        int count;

        Counter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 1;
        }
    }
}
