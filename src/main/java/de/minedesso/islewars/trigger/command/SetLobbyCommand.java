package de.minedesso.islewars.trigger.command;

import de.minedesso.islewars.application.lobby.LobbyCoordinator;
import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.port.out.IsleWarsApiPort;
import de.minedesso.islewars.application.port.out.TaskScheduler;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.util.Message;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletionException;

public final class SetLobbyCommand implements CommandExecutor {
    private final IsleWarsApiPort apiPort;
    private final ServerRuntimeService runtimeService;
    private final LobbyPlayerService playerService;
    private final LobbyCoordinator lobbyCoordinator;
    private final TaskScheduler scheduler;

    public SetLobbyCommand(
            IsleWarsApiPort apiPort,
            ServerRuntimeService runtimeService,
            LobbyPlayerService playerService,
            LobbyCoordinator lobbyCoordinator,
            TaskScheduler scheduler
    ) {
        this.apiPort = apiPort;
        this.runtimeService = runtimeService;
        this.playerService = playerService;
        this.lobbyCoordinator = lobbyCoordinator;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.ERROR.with(
                    "Dieser Befehl kann nur von einem Spieler ausgeführt werden."));
            return true;
        }
        if (args != null && args.length > 0) {
            player.sendMessage(Message.ERROR.with("Verwendung: /setlobby"));
            return true;
        }

        Location location = player.getLocation();
        LobbySpawn lobbySpawn = new LobbySpawn(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        player.sendMessage(Message.WARNING.with("Lobby-Spawn wird gespeichert ..."));

        this.apiPort.saveLobbySpawn(lobbySpawn).whenComplete((ignored, throwable) ->
                this.scheduler.runSync(() -> {
                    if (throwable != null) {
                        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                                ? throwable.getCause()
                                : throwable;
                        player.sendMessage(Message.ERROR.with(
                                "Lobby-Spawn konnte nicht gespeichert werden: " + cause.getMessage()));
                        return;
                    }

                    boolean becameReady = this.runtimeService.updateLobbySpawn(
                            lobbySpawn, this.playerService::isSpawnAvailable);
                    player.sendMessage(Message.SUCCESS.with("Lobby-Spawn wurde gespeichert."));
                    if (becameReady) {
                        this.lobbyCoordinator.activateReadyLobby();
                    }
                }));
        return true;
    }
}
