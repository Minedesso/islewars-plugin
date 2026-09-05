package de.minedesso.islewars.application.lobby;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

public interface ProtectedInventoryHolder extends InventoryHolder {
    void handleClick(Player player, int rawSlot, ClickType clickType);
}
