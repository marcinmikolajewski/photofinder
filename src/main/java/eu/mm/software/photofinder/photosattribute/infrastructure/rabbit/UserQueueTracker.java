package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Śledzi ile wiadomości wysłał każdy użytkownik na kolejkę.
 * Priority maleje wraz z liczbą wysłanych wiadomości —
 * nowy użytkownik zawsze startuje z wysokim priorytetem.
 */
@Service
public class UserQueueTracker {

    private final ConcurrentHashMap<String, AtomicLong> userCounters = new ConcurrentHashMap<>();

    public UserQueueTracker(MeterRegistry registry) {
        // gauge pokazujący ile userów wysyła zdjęcia w tej chwili
        Gauge.builder("queue.active.users", userCounters, Map::size)
                .description("Number of users currently sending photos to queue")
                .register(registry);
    }

    public int nextPriority(String userId) {
        long position = userCounters
                .computeIfAbsent(userId, k -> new AtomicLong(0))
                .getAndIncrement();

        return calculatePriority(position);
    }

    private int calculatePriority(long position) {
        if (position < 5) return 25; // ~12 sek przy 2 konsumentach
        if (position < 30) return 20; // ~75 sek
        if (position < 150) return 10; // ~6 min
        if (position < 600) return 5; // ~25 min
        if (position < 2000) return 3;
        return 1;
    }

    /**
     * Reset liczników co noc — user A który wysłał 10 000 zdjęć wczoraj
     * nie powinien mieć kary dziś rano.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void resetCounters() {
        userCounters.clear();
    }
}