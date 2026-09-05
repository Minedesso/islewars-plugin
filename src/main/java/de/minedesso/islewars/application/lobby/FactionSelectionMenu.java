package de.minedesso.islewars.application.lobby;

import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.Faction;
import de.minedesso.islewars.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FactionSelectionMenu {
    public static final String TITLE = "Fraktionswahl";
    public static final int SIZE = 54;

    private static final Map<Faction, Integer> FACTION_SLOTS = factionSlots();
    private static final Map<Faction, Material> BANNERS = banners();
    private static final Map<Faction, Integer> RESOURCE_SLOTS = resourceSlots();

    private final ServerRuntimeService runtimeService;
    private final FactionSelectionService selectionService;

    public FactionSelectionMenu(
            ServerRuntimeService runtimeService,
            FactionSelectionService selectionService
    ) {
        this.runtimeService = runtimeService;
        this.selectionService = selectionService;
    }

    public void open(Player player) {
        FactionInventoryHolder holder = new FactionInventoryHolder(player.getUniqueId());
        this.render(holder);
        player.openInventory(holder.getInventory());
    }

    public void refreshOpenMenus() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof FactionInventoryHolder holder) {
                this.render(holder);
            }
        });
    }

    public void closeOpenMenus() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof FactionInventoryHolder) {
                player.closeInventory();
            }
        });
    }

    private void select(Player player, Faction faction) {
        int capacity = this.runtimeService.requireConfiguration().mode().factionCapacity();
        FactionSelectionService.SelectionResult result = this.selectionService.select(
                player.getUniqueId(), faction, capacity);
        switch (result) {
            case SELECTED -> player.sendMessage(Message.SUCCESS.with(
                    "Du kämpfst nun für die Fraktion " + faction.displayName() + "."));
            case ALREADY_SELECTED -> player.sendMessage(Message.WARNING.with(
                    "Du gehörst bereits zur Fraktion " + faction.displayName() + "."));
            case FULL -> player.sendMessage(Message.ERROR.with(
                    "Die Fraktion " + faction.displayName() + " ist bereits voll."));
        }
        this.refreshOpenMenus();
    }

    private void render(FactionInventoryHolder holder) {
        Inventory inventory = holder.getInventory();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(22, item(Material.GRASS_BLOCK, ChatColor.GREEN + "IsleWars-Mitte",
                List.of(ChatColor.GRAY + "Das Herzstück der Karte")));
        inventory.setItem(31, item(Material.BEACON, ChatColor.AQUA + "Zentraler Nexus",
                List.of(ChatColor.GRAY + "Hier treffen die Fraktionen aufeinander")));

        int capacity = this.runtimeService.requireConfiguration().mode().factionCapacity();
        for (Faction faction : Faction.values()) {
            inventory.setItem(FACTION_SLOTS.get(faction), this.banner(faction, holder.viewerId, capacity));
            inventory.setItem(RESOURCE_SLOTS.get(faction), item(
                    Material.TUFF,
                    ChatColor.GOLD + "Ressourceninsel von " + faction.displayName(),
                    List.of(
                            ChatColor.GRAY + "Vorkommen: Kupfer, Kohle und Eisen",
                            ChatColor.DARK_GRAY + "Nahe der Fraktion " + faction.displayName()
                    )
            ));
        }
    }

    private ItemStack banner(Faction faction, UUID viewerId, int capacity) {
        boolean selected = this.selectionService.factionOf(viewerId).filter(faction::equals).isPresent();
        int size = this.selectionService.size(faction);
        String status = selected
                ? ChatColor.GREEN + "Ausgewählt"
                : size >= capacity
                ? ChatColor.RED + "Voll"
                : ChatColor.YELLOW + "Klicke zum Beitreten";
        ItemStack banner = item(
                BANNERS.get(faction),
                factionColor(faction) + faction.displayName(),
                List.of(
                        ChatColor.GRAY + "Stärke: " + ChatColor.WHITE + faction.strength(),
                        ChatColor.GRAY + "Spieler: " + ChatColor.YELLOW + size + "/" + capacity,
                        "",
                        status
                )
        );
        if (selected) {
            banner.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            ItemMeta meta = banner.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            banner.setItemMeta(meta);
        }
        return banner;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ChatColor factionColor(Faction faction) {
        return switch (faction) {
            case FORTITUDO -> ChatColor.RED;
            case CELERITAS -> ChatColor.YELLOW;
            case SAPIENTIA -> ChatColor.BLUE;
            case TENACITAS -> ChatColor.GREEN;
        };
    }

    private static Map<Faction, Integer> factionSlots() {
        Map<Faction, Integer> slots = new EnumMap<>(Faction.class);
        slots.put(Faction.FORTITUDO, 18);
        slots.put(Faction.CELERITAS, 4);
        slots.put(Faction.SAPIENTIA, 35);
        slots.put(Faction.TENACITAS, 49);
        return Map.copyOf(slots);
    }

    private static Map<Faction, Material> banners() {
        Map<Faction, Material> banners = new EnumMap<>(Faction.class);
        banners.put(Faction.FORTITUDO, Material.RED_BANNER);
        banners.put(Faction.CELERITAS, Material.YELLOW_BANNER);
        banners.put(Faction.SAPIENTIA, Material.BLUE_BANNER);
        banners.put(Faction.TENACITAS, Material.GREEN_BANNER);
        return Map.copyOf(banners);
    }

    private static Map<Faction, Integer> resourceSlots() {
        Map<Faction, Integer> slots = new EnumMap<>(Faction.class);
        slots.put(Faction.FORTITUDO, 28);
        slots.put(Faction.CELERITAS, 12);
        slots.put(Faction.SAPIENTIA, 25);
        slots.put(Faction.TENACITAS, 41);
        return Map.copyOf(slots);
    }

    private final class FactionInventoryHolder implements ProtectedInventoryHolder {
        private final UUID viewerId;
        private final Inventory inventory;

        private FactionInventoryHolder(UUID viewerId) {
            this.viewerId = viewerId;
            this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }

        @Override
        public void handleClick(Player player, int rawSlot, ClickType clickType) {
            if (!this.viewerId.equals(player.getUniqueId())
                    || (clickType != ClickType.LEFT && clickType != ClickType.RIGHT)) {
                return;
            }
            FACTION_SLOTS.entrySet().stream()
                    .filter(entry -> entry.getValue() == rawSlot)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .ifPresent(faction -> FactionSelectionMenu.this.select(player, faction));
        }
    }
}
