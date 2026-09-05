package de.minedesso.islewars.trigger.listener;

import de.minedesso.islewars.application.lobby.ProtectedInventoryHolder;
import de.minedesso.islewars.application.service.GameStateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

public final class InventoryProtectionListener implements Listener {
    private final GameStateService gameStateService;

    public InventoryProtectionListener(GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory topInventory = event.getView().getTopInventory();
        ProtectedInventoryHolder holder = topInventory.getHolder() instanceof ProtectedInventoryHolder protectedHolder
                ? protectedHolder
                : null;
        if (holder == null && !this.lobbyInventoryProtected(player)) {
            return;
        }

        event.setCancelled(true);
        if (holder != null && event.getRawSlot() >= 0 && event.getRawSlot() < topInventory.getSize()) {
            holder.handleClick(player, event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && (this.hasProtectedInventoryOpen(player) || this.lobbyInventoryProtected(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (this.hasProtectedInventoryOpen(event.getPlayer()) || this.lobbyInventoryProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (this.hasProtectedInventoryOpen(event.getPlayer()) || this.lobbyInventoryProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean hasProtectedInventoryOpen(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof ProtectedInventoryHolder;
    }

    private boolean lobbyInventoryProtected(Player player) {
        return player.isOnline() && this.gameStateService.isLobbyProtectionActive();
    }
}
