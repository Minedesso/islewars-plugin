package de.minedesso.islewars.infrastructure.bukkit;

import de.minedesso.islewars.application.port.out.LobbyAudience;
import org.bukkit.Bukkit;

public final class BukkitLobbyAudience implements LobbyAudience {
    @Override
    public int playerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public void broadcast(String message) {
        Bukkit.broadcastMessage(message);
    }
}
