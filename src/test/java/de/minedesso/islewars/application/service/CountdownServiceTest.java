package de.minedesso.islewars.application.service;

import de.minedesso.islewars.application.port.out.LobbyAudience;
import de.minedesso.islewars.application.port.out.TaskScheduler;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountdownServiceTest {
    private final ServerRuntimeService runtimeService = new ServerRuntimeService();
    private final MutableAudience audience = new MutableAudience();
    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();
    private final AtomicInteger starts = new AtomicInteger();
    private CountdownService service;

    @BeforeEach
    void setUp() {
        ServerConfiguration configuration = new ServerConfiguration(
                IsleWarsMode.FOUR_BY_TWO,
                3,
                8,
                60,
                new LobbySpawn("world", 0, 64, 0, 0, 0)
        );
        this.runtimeService.applyConfiguration(configuration, ignored -> true);
        this.service = new CountdownService(
                this.runtimeService, this.audience, this.scheduler, this.starts::incrementAndGet);
    }

    @Test
    void startsAndCancelsAutomaticCountdownAtConfiguredMinimum() {
        this.audience.playerCount = 3;
        this.service.onPlayerCountChanged(3);

        assertEquals(CountdownService.Status.RUNNING, this.service.status());
        assertFalse(this.service.isForced());
        assertEquals(60, this.service.remainingSeconds());

        this.audience.playerCount = 2;
        this.service.onPlayerCountChanged(2);

        assertEquals(CountdownService.Status.WAITING, this.service.status());
        assertEquals(0, this.scheduler.activeTaskCount());

        this.audience.playerCount = 3;
        this.service.onPlayerCountChanged(3);
        assertEquals(CountdownService.Status.RUNNING, this.service.status());
        assertEquals(1, this.scheduler.activeTaskCount());
    }

    @Test
    void rejectsForceStartWithOnlyOnePlayer() {
        this.audience.playerCount = 1;

        assertEquals(CountdownService.ForceStartResult.NOT_ENOUGH_PLAYERS,
                this.service.forceStart("Alice"));
        assertEquals(CountdownService.Status.WAITING, this.service.status());
        assertTrue(this.audience.messages.isEmpty());
    }

    @Test
    void forcedCountdownStartsWithTwoPlayersAndCompletesAfterThreeTicks() {
        this.audience.playerCount = 2;

        assertEquals(CountdownService.ForceStartResult.STARTED, this.service.forceStart("Alice"));
        assertTrue(this.service.isForced());
        assertEquals(3, this.service.remainingSeconds());

        this.scheduler.tickActiveTasks();
        this.scheduler.tickActiveTasks();
        this.scheduler.tickActiveTasks();

        assertEquals(CountdownService.Status.START_SIMULATED, this.service.status());
        assertEquals(1, this.starts.get());
        assertEquals(0, this.scheduler.activeTaskCount());
    }

    @Test
    void forcedCountdownCancelsBelowTwoAndDoesNotRestartAtTwoBelowApiMinimum() {
        this.audience.playerCount = 2;
        this.service.forceStart("Alice");

        this.audience.playerCount = 1;
        this.service.onPlayerCountChanged(1);
        assertEquals(CountdownService.Status.WAITING, this.service.status());

        this.audience.playerCount = 2;
        this.service.onPlayerCountChanged(2);
        assertEquals(CountdownService.Status.WAITING, this.service.status());
        assertEquals(0, this.scheduler.activeTaskCount());
    }

    @Test
    void forceStartConvertsExistingCountdownWithoutCreatingAnotherTask() {
        this.audience.playerCount = 3;
        this.service.onPlayerCountChanged(3);
        assertEquals(1, this.scheduler.activeTaskCount());

        assertEquals(CountdownService.ForceStartResult.STARTED, this.service.forceStart("Bob"));

        assertTrue(this.service.isForced());
        assertEquals(3, this.service.remainingSeconds());
        assertEquals(1, this.scheduler.activeTaskCount());
        assertEquals(CountdownService.ForceStartResult.ALREADY_RUNNING, this.service.forceStart("Alice"));
        assertEquals(1, this.scheduler.activeTaskCount());
    }

    private static final class MutableAudience implements LobbyAudience {
        private int playerCount;
        private final List<String> messages = new ArrayList<>();

        @Override
        public int playerCount() {
            return this.playerCount;
        }

        @Override
        public void broadcast(String message) {
            this.messages.add(message);
        }
    }

    private static final class FakeTaskScheduler implements TaskScheduler {
        private final List<FakeTask> tasks = new ArrayList<>();

        @Override
        public ScheduledTask runRepeating(Runnable runnable, long delayTicks, long periodTicks) {
            FakeTask task = new FakeTask(runnable);
            this.tasks.add(task);
            return task;
        }

        @Override
        public void runSync(Runnable runnable) {
            runnable.run();
        }

        private void tickActiveTasks() {
            List.copyOf(this.tasks).stream()
                    .filter(task -> !task.cancelled)
                    .forEach(task -> task.runnable.run());
        }

        private long activeTaskCount() {
            return this.tasks.stream().filter(task -> !task.cancelled).count();
        }

        private static final class FakeTask implements ScheduledTask {
            private final Runnable runnable;
            private boolean cancelled;

            private FakeTask(Runnable runnable) {
                this.runnable = runnable;
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return this.cancelled;
            }
        }
    }
}
