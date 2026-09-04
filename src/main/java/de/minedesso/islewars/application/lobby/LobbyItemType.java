package de.minedesso.islewars.application.lobby;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;

public enum LobbyItemType {
    MAP_SELECTION(0, Material.COMPASS, "Map-Auswahl"),
    TEAM_SELECTION(4, Material.PLAYER_HEAD, "Team-Auswahl"),
    FUN_AREA(8, Material.SLIME_BALL, "Spaß-Area");

    private final int slot;
    private final Material material;
    private final String displayName;

    LobbyItemType(int slot, Material material, String displayName) {
        this.slot = slot;
        this.material = material;
        this.displayName = displayName;
    }

    public int slot() {
        return this.slot;
    }

    public ItemStack createItem() {
        ItemStack itemStack = new ItemStack(this.material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GOLD + this.displayName);
        itemMeta.setUnbreakable(true);
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean matches(ItemStack itemStack) {
        return itemStack != null && this.createItem().isSimilar(itemStack);
    }

    public static Optional<LobbyItemType> fromSlotAndItem(int slot, ItemStack itemStack) {
        return Arrays.stream(values())
                .filter(type -> type.slot == slot && type.matches(itemStack))
                .findFirst();
    }
}
