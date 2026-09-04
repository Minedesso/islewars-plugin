package de.minedesso.islewars.application.port.out;

public interface TaskScheduler {
    ScheduledTask runRepeating(Runnable runnable, long delayTicks, long periodTicks);

    void runSync(Runnable runnable);

    interface ScheduledTask {
        void cancel();

        boolean isCancelled();
    }
}
