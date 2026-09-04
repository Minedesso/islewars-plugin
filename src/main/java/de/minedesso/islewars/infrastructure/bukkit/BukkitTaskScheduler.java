package de.minedesso.islewars.infrastructure.bukkit;

import de.minedesso.islewars.application.port.out.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BukkitTaskScheduler implements TaskScheduler {
    private final JavaPlugin plugin;

    public BukkitTaskScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public ScheduledTask runRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override
    public void runSync(Runnable runnable) {
        if (!this.plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(this.plugin, runnable);
        }
    }

    private record BukkitScheduledTask(BukkitTask task) implements ScheduledTask {
        @Override
        public void cancel() {
            this.task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return this.task.isCancelled();
        }
    }
}
