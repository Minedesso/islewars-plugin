package de.minedesso.islewars.infrastructure.bukkit;

import de.minedesso.islewars.application.lobby.FactionSelectionMenu;
import de.minedesso.islewars.application.port.in.GameStartAction;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class BukkitGameStartAction implements GameStartAction {
    private final ServerRuntimeService runtimeService;
    private final FactionSelectionService factionSelectionService;
    private final FactionSelectionMenu factionSelectionMenu;

    public BukkitGameStartAction(
            ServerRuntimeService runtimeService,
            FactionSelectionService factionSelectionService,
            FactionSelectionMenu factionSelectionMenu
    ) {
        this.runtimeService = runtimeService;
        this.factionSelectionService = factionSelectionService;
        this.factionSelectionMenu = factionSelectionMenu;
    }

    @Override
    public void startGame() {
        List<UUID> playerIds = Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .toList();
        this.factionSelectionService.assignUnselected(
                        playerIds,
                        this.runtimeService.requireConfiguration().mode().factionCapacity())
                .forEach((playerId, faction) -> {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(Message.WARNING.with("Du wurdest automatisch der Fraktion "
                                + faction.displayName() + " zugeordnet."));
                    }
                });
        this.factionSelectionMenu.closeOpenMenus();
        Bukkit.broadcastMessage(Message.SUCCESS.with("Das Spiel startet jetzt!"));
    }
}
