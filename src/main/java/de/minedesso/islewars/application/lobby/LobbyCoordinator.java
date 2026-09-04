package de.minedesso.islewars.application.lobby;

import de.minedesso.islewars.application.service.CountdownService;
import org.bukkit.Bukkit;

public final class LobbyCoordinator {
    private final LobbyPlayerService playerService;
    private final CountdownService countdownService;

    public LobbyCoordinator(LobbyPlayerService playerService, CountdownService countdownService) {
        this.playerService = playerService;
        this.countdownService = countdownService;
    }

    public void activateReadyLobby() {
        Bukkit.getOnlinePlayers().forEach(this.playerService::prepareLobbyPlayer);
        this.countdownService.onPlayerCountChanged(Bukkit.getOnlinePlayers().size());
    }
}
