package de.minedesso.islewars.application.service;

import de.minedesso.islewars.application.port.out.IsleWarsApiPort;
import de.minedesso.islewars.application.port.out.TaskScheduler;
import de.minedesso.islewars.domain.model.LobbySpawn;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BootstrapService {
    private final IsleWarsApiPort apiPort;
    private final ServerRuntimeService runtimeService;
    private final TaskScheduler scheduler;
    private final long retryTicks;
    private final Predicate<LobbySpawn> spawnAvailable;
    private final Runnable onReady;
    private final Logger logger;
    private final AtomicBoolean requestInFlight = new AtomicBoolean();

    private TaskScheduler.ScheduledTask retryTask;
    private boolean stopped;

    public BootstrapService(
            IsleWarsApiPort apiPort,
            ServerRuntimeService runtimeService,
            TaskScheduler scheduler,
            long retryTicks,
            Predicate<LobbySpawn> spawnAvailable,
            Runnable onReady,
            Logger logger
    ) {
        this.apiPort = apiPort;
        this.runtimeService = runtimeService;
        this.scheduler = scheduler;
        this.retryTicks = Math.max(20L, retryTicks);
        this.spawnAvailable = spawnAvailable;
        this.onReady = onReady;
        this.logger = logger;
    }

    public void start() {
        this.retryTask = this.scheduler.runRepeating(this::attemptBootstrap, this.retryTicks, this.retryTicks);
        this.attemptBootstrap();
    }

    public void stop() {
        this.stopped = true;
        if (this.retryTask != null) {
            this.retryTask.cancel();
            this.retryTask = null;
        }
    }

    private void attemptBootstrap() {
        if (this.stopped || this.runtimeService.isReady() || !this.requestInFlight.compareAndSet(false, true)) {
            return;
        }

        this.apiPort.fetchServerConfiguration().whenComplete((configuration, throwable) ->
                this.scheduler.runSync(() -> {
                    this.requestInFlight.set(false);
                    if (this.stopped) {
                        return;
                    }
                    if (throwable != null) {
                        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                                ? throwable.getCause()
                                : throwable;
                        this.logger.log(Level.WARNING,
                                "IsleWars-Serverkonfiguration konnte nicht geladen werden; neuer Versuch folgt: "
                                        + cause.getMessage());
                        return;
                    }

                    boolean becameReady = this.runtimeService.applyConfiguration(configuration, this.spawnAvailable);
                    if (becameReady) {
                        if (this.retryTask != null) {
                            this.retryTask.cancel();
                            this.retryTask = null;
                        }
                        this.logger.info("IsleWars-Lobby ist bereit.");
                        this.onReady.run();
                    } else {
                        this.logger.warning("IsleWars-Konfiguration geladen, aber der Lobby-Spawn fehlt oder seine Welt ist nicht geladen.");
                    }
                }));
    }
}
