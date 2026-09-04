package de.minedesso.islewars.application.service;

import de.minedesso.islewars.application.port.in.GameStartAction;
import de.minedesso.islewars.application.port.out.LobbyAudience;
import de.minedesso.islewars.application.port.out.TaskScheduler;
import de.minedesso.islewars.domain.model.ServerConfiguration;

import java.util.Set;

public final class CountdownService {
    private static final int FORCED_MINIMUM_PLAYERS = 2;
    private static final int FORCED_SECONDS = 3;
    private static final Set<Integer> MILESTONES = Set.of(60, 30, 15, 10, 5, 4, 3, 2, 1);

    private final ServerRuntimeService runtimeService;
    private final LobbyAudience audience;
    private final TaskScheduler scheduler;
    private final GameStartAction gameStartAction;

    private Status status = Status.WAITING;
    private boolean forced;
    private int remainingSeconds;
    private TaskScheduler.ScheduledTask task;

    public CountdownService(
            ServerRuntimeService runtimeService,
            LobbyAudience audience,
            TaskScheduler scheduler,
            GameStartAction gameStartAction
    ) {
        this.runtimeService = runtimeService;
        this.audience = audience;
        this.scheduler = scheduler;
        this.gameStartAction = gameStartAction;
    }

    public synchronized void onPlayerCountChanged(int playerCount) {
        if (!this.runtimeService.isReady() || this.status == Status.START_SIMULATED) {
            return;
        }

        if (this.status == Status.RUNNING) {
            int requiredPlayers = this.forced
                    ? FORCED_MINIMUM_PLAYERS
                    : this.runtimeService.requireConfiguration().minimumPlayers();
            if (playerCount < requiredPlayers) {
                this.cancelCountdown();
                this.audience.broadcast("§cDer Countdown wurde abgebrochen: Es sind zu wenige Spieler in der Lobby.");
            }
            return;
        }

        ServerConfiguration configuration = this.runtimeService.requireConfiguration();
        if (playerCount >= configuration.minimumPlayers()) {
            this.startCountdown(configuration.countdownSeconds(), false);
        }
    }

    public synchronized ForceStartResult forceStart(String playerName) {
        if (!this.runtimeService.isReady()) {
            return ForceStartResult.NOT_READY;
        }
        if (this.status == Status.START_SIMULATED) {
            return ForceStartResult.ALREADY_STARTED;
        }
        if (this.audience.playerCount() < FORCED_MINIMUM_PLAYERS) {
            return ForceStartResult.NOT_ENOUGH_PLAYERS;
        }
        if (this.status == Status.RUNNING && this.forced && this.remainingSeconds <= FORCED_SECONDS) {
            return ForceStartResult.ALREADY_RUNNING;
        }

        this.audience.broadcast("§e" + playerName
                + " war wohl zu ungeduldig – das Spiel startet in drei Sekunden!");

        if (this.status == Status.RUNNING) {
            this.forced = true;
            this.remainingSeconds = Math.min(this.remainingSeconds, FORCED_SECONDS);
        } else {
            this.startCountdown(FORCED_SECONDS, true);
        }
        return ForceStartResult.STARTED;
    }

    public synchronized Status status() {
        return this.status;
    }

    public synchronized int remainingSeconds() {
        return this.remainingSeconds;
    }

    public synchronized boolean isForced() {
        return this.forced;
    }

    public synchronized void shutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        if (this.status == Status.RUNNING) {
            this.status = Status.WAITING;
        }
    }

    private void startCountdown(int seconds, boolean forced) {
        this.status = Status.RUNNING;
        this.forced = forced;
        this.remainingSeconds = seconds;
        if (!forced) {
            this.audience.broadcast("§eDas Spiel startet in " + seconds + " Sekunden.");
        }
        this.task = this.scheduler.runRepeating(this::tick, 20L, 20L);
    }

    private synchronized void tick() {
        if (this.status != Status.RUNNING) {
            return;
        }

        int requiredPlayers = this.forced
                ? FORCED_MINIMUM_PLAYERS
                : this.runtimeService.requireConfiguration().minimumPlayers();
        if (this.audience.playerCount() < requiredPlayers) {
            this.cancelCountdown();
            this.audience.broadcast("§cDer Countdown wurde abgebrochen: Es sind zu wenige Spieler in der Lobby.");
            return;
        }

        this.remainingSeconds--;
        if (this.remainingSeconds <= 0) {
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            this.status = Status.START_SIMULATED;
            this.gameStartAction.startGame();
            return;
        }

        if (MILESTONES.contains(this.remainingSeconds)) {
            this.audience.broadcast("§eDas Spiel startet in " + this.remainingSeconds
                    + (this.remainingSeconds == 1 ? " Sekunde." : " Sekunden."));
        }
    }

    private void cancelCountdown() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.status = Status.WAITING;
        this.forced = false;
        this.remainingSeconds = 0;
    }

    public enum Status {
        WAITING,
        RUNNING,
        START_SIMULATED
    }

    public enum ForceStartResult {
        STARTED,
        NOT_READY,
        NOT_ENOUGH_PLAYERS,
        ALREADY_RUNNING,
        ALREADY_STARTED
    }
}
