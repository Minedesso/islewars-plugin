package de.minedesso.islewars.application.lobby;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface LobbyItemAction {
    void execute(Player player);
}
