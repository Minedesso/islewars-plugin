package de.minedesso.islewars.trigger.listener;

import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.service.CountdownService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public final class PlayerLifecycleListener implements Listener {
    public static final String SET_LOBBY_PERMISSION = "islewars.command.setlobby";

    private final ServerRuntimeService runtimeService;
    private final LobbyPlayerService playerService;
    private final CountdownService countdownService;

    public PlayerLifecycleListener(
            ServerRuntimeService runtimeService,
            LobbyPlayerService playerService,
            CountdownService countdownService
    ) {
        this.runtimeService = runtimeService;
        this.playerService = playerService;
        this.countdownService = countdownService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (!this.runtimeService.isReady()) {
            return;
        }
        int maximumPlayers = this.runtimeService.requireConfiguration().maximumPlayers();
        if (Bukkit.getOnlinePlayers().size() >= maximumPlayers) {
            event.disallow(PlayerLoginEvent.Result.KICK_FULL,
                    Message.ERROR.with("Die IsleWars-Lobby ist bereits voll."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.runtimeService.isReady()) {
            if (event.getPlayer().hasPermission(SET_LOBBY_PERMISSION)) {
                this.playerService.prepareSetupPlayer(event.getPlayer());
                return;
            }
            event.setJoinMessage(null);
            event.getPlayer().kickPlayer(Message.ERROR.with(
                    "Die IsleWars-Lobby wird noch initialisiert. Bitte versuche es gleich erneut."));
            return;
        }

        this.playerService.prepareLobbyPlayer(event.getPlayer());
        this.countdownService.onPlayerCountChanged(Bukkit.getOnlinePlayers().size());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (this.runtimeService.isReady()) {
            UUID leavingPlayerId = event.getPlayer().getUniqueId();
            int remainingPlayers = (int) Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !player.getUniqueId().equals(leavingPlayerId))
                    .count();
            this.countdownService.onPlayerCountChanged(remainingPlayers);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (this.runtimeService.isReady()) {
            event.setRespawnLocation(this.playerService.requireLobbyLocation());
        }
    }
}
