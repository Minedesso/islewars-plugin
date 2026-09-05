package de.minedesso.islewars.application.lobby;

import de.minedesso.islewars.util.Message;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class LobbyItemService {
    private final Map<LobbyItemType, LobbyItemAction> actions;

    public LobbyItemService(Map<LobbyItemType, LobbyItemAction> actions) {
        this.actions = new EnumMap<>(LobbyItemType.class);
        this.actions.putAll(actions);
        for (LobbyItemType type : LobbyItemType.values()) {
            if (!this.actions.containsKey(type)) {
                throw new IllegalArgumentException("Keine Action für Lobby-Item " + type + " registriert.");
            }
        }
    }

    public static LobbyItemService withPlaceholderActions() {
        return withTeamSelectionAction(player ->
                player.sendMessage(Message.WARNING.with("Die Team-Auswahl ist noch nicht verfügbar.")));
    }

    public static LobbyItemService withTeamSelectionAction(LobbyItemAction teamSelectionAction) {
        Objects.requireNonNull(teamSelectionAction, "teamSelectionAction");
        Map<LobbyItemType, LobbyItemAction> actions = new EnumMap<>(LobbyItemType.class);
        actions.put(LobbyItemType.MAP_SELECTION, player ->
                player.sendMessage(Message.WARNING.with("Die Map-Auswahl ist noch nicht verfügbar.")));
        actions.put(LobbyItemType.TEAM_SELECTION, teamSelectionAction);
        actions.put(LobbyItemType.FUN_AREA, player ->
                player.sendMessage(Message.WARNING.with("Die Spaß-Area ist noch nicht verfügbar.")));
        return new LobbyItemService(actions);
    }

    public void giveItems(Player player) {
        for (LobbyItemType type : LobbyItemType.values()) {
            player.getInventory().setItem(type.slot(), type.createItem());
        }
    }

    public boolean executeFor(Player player, int heldSlot, ItemStack itemStack) {
        Objects.requireNonNull(player, "player");
        return LobbyItemType.fromSlotAndItem(heldSlot, itemStack)
                .map(type -> {
                    this.actions.get(type).execute(player);
                    return true;
                })
                .orElse(false);
    }
}
