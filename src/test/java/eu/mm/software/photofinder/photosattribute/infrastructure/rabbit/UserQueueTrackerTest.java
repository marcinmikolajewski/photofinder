package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserQueueTrackerTest {

    private UserQueueTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new UserQueueTracker(new SimpleMeterRegistry());
    }

    @Test
    void nextPriority_returnsHighestPriority_forFirstMessage() {
        // given
        String userId = "user-1";

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(25);
    }

    @Test
    void nextPriority_returnsHighestPriority_forFirst5Messages() {
        // given
        String userId = "user-1";

        // when / then
        for (int i = 0; i < 5; i++) {
            assertThat(tracker.nextPriority(userId)).isEqualTo(25);
        }
    }

    @Test
    void nextPriority_dropsPriority_afterFirst5Messages() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 5; i++) {
            tracker.nextPriority(userId);
        }

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(20);
    }

    @Test
    void nextPriority_dropsPriority_after30Messages() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 30; i++) {
            tracker.nextPriority(userId);
        }

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(10);
    }

    @Test
    void nextPriority_dropsPriority_after150Messages() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 150; i++) {
            tracker.nextPriority(userId);
        }

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(5);
    }

    @Test
    void nextPriority_dropsPriority_after600Messages() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 600; i++) {
            tracker.nextPriority(userId);
        }

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(3);
    }

    @Test
    void nextPriority_dropsToMinimum_after2000Messages() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 2000; i++) {
            tracker.nextPriority(userId);
        }

        // when
        int priority = tracker.nextPriority(userId);

        // then
        assertThat(priority).isEqualTo(1);
    }

    @Test
    void nextPriority_tracksCountersSeparatelyPerUser() {
        // given
        String heavyUser = "heavy-user";
        String newUser = "new-user";
        for (int i = 0; i < 100; i++) {
            tracker.nextPriority(heavyUser);
        }

        // when
        int priorityForNewUser = tracker.nextPriority(newUser);
        int priorityForHeavyUser = tracker.nextPriority(heavyUser);

        // then
        assertThat(priorityForNewUser).isEqualTo(25);
        assertThat(priorityForHeavyUser).isEqualTo(10);
    }

    @Test
    void resetCounters_restoresHighestPriority_forPreviouslyHeavyUser() {
        // given
        String userId = "user-1";
        for (int i = 0; i < 500; i++) {
            tracker.nextPriority(userId);
        }
        assertThat(tracker.nextPriority(userId)).isEqualTo(5);

        // when
        tracker.resetCounters();

        // then
        assertThat(tracker.nextPriority(userId)).isEqualTo(25);
    }
}
