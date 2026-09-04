package de.minedesso.islewars.trigger.command;

import de.minedesso.islewars.application.lobby.LobbyCoordinator;
import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.port.out.IsleWarsServerRepository;
import de.minedesso.islewars.application.port.out.TaskScheduler;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.LobbySpawn;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletionException;

public final class SetLobbyCommand implements CommandExecutor {
    private final IsleWarsServerRepository repository;
    private final ServerRuntimeService runtimeService;
    private final LobbyPlayerService playerService;
    private final LobbyCoordinator lobbyCoordinator;
    private final TaskScheduler scheduler;

    public SetLobbyCommand(
            IsleWarsServerRepository repository,
            ServerRuntimeService runtimeService,
            LobbyPlayerService playerService,
            LobbyCoordinator lobbyCoordinator,
            TaskScheduler scheduler
    ) {
        this.repository = repository;
        this.runtimeService = runtimeService;
        this.playerService = playerService;
        this.lobbyCoordinator = lobbyCoordinator;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }
        if (args != null && args.length > 0) {
            player.sendMessage(ChatColor.RED + "Verwendung: /setlobby");
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
        player.sendMessage(ChatColor.YELLOW + "Lobby-Spawn wird gespeichert ...");

        this.repository.saveLobbySpawn(lobbySpawn).whenComplete((ignored, throwable) ->
                this.scheduler.runSync(() -> {
                    if (throwable != null) {
                        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                                ? throwable.getCause()
                                : throwable;
                        player.sendMessage(ChatColor.RED + "Lobby-Spawn konnte nicht gespeichert werden: "
                                + cause.getMessage());
                        return;
                    }

                    boolean becameReady = this.runtimeService.updateLobbySpawn(
                            lobbySpawn, this.playerService::isSpawnAvailable);
                    player.sendMessage(ChatColor.GREEN + "Lobby-Spawn wurde gespeichert.");
                    if (becameReady) {
                        this.lobbyCoordinator.activateReadyLobby();
                    }
                }));
        return true;
    }
}
