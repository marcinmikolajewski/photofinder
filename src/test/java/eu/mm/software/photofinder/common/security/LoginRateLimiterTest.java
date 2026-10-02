package eu.mm.software.photofinder.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void allowsUpToLimit_thenBlocks() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60_000);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 1
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 2
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 3
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse(); // 4 — przekroczono
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse(); // dalej blokada
    }

    @Test
    void keysAreIndependent() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60_000);

        assertThat(limiter.tryAcquire("ip-a")).isTrue();
        assertThat(limiter.tryAcquire("ip-a")).isFalse(); // a wyczerpane
        assertThat(limiter.tryAcquire("ip-b")).isTrue();   // b ma własny licznik
    }

    @Test
    void resetsAfterWindow() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 30);

        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse();
        Thread.sleep(60); // okno wygasa
        assertThat(limiter.tryAcquire("ip")).isTrue(); // nowe okno
    }
}
