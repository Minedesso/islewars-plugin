package de.minedesso.islewars.application.lobby;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.Faction;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionSelectionMenuTest {
    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.server.addSimpleWorld("world");
        this.player = this.server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @CsvSource({
            "FOUR_BY_ONE, 1",
            "FOUR_BY_TWO, 2",
            "FOUR_BY_THREE, 3"
    })
    void rendersMapAndModeCapacity(IsleWarsMode mode, int capacity) {
        FactionSelectionMenu menu = menu(mode, new FactionSelectionService());
        menu.open(this.player);
        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        assertEquals(FactionSelectionMenu.SIZE, inventory.getSize());
        assertEquals("Fraktionswahl", FactionSelectionMenu.TITLE);
        assertEquals(Material.YELLOW_BANNER, inventory.getItem(4).getType());
        assertEquals(Material.RED_BANNER, inventory.getItem(18).getType());
        assertEquals(Material.BLUE_BANNER, inventory.getItem(26).getType());
        assertEquals(Material.GREEN_BANNER, inventory.getItem(49).getType());
        assertEquals(Material.GRASS_BLOCK, inventory.getItem(22).getType());
        assertEquals(Material.BEACON, inventory.getItem(31).getType());
        assertResourceIsland(inventory, 12);
        assertResourceIsland(inventory, 28);
        assertResourceIsland(inventory, 34);
        assertResourceIsland(inventory, 41);
        assertEquals(4, Arrays.stream(inventory.getContents())
                .filter(item -> item != null && item.getType() == Material.TUFF)
                .count());
        assertTrue(inventory.getItem(12).getItemMeta().getDisplayName().contains("Ressourceninsel"));
        assertTrue(inventory.getItem(12).getItemMeta().getLore().stream()
                .anyMatch(line -> line.contains("Kupfer, Kohle und Eisen")));
        assertTrue(inventory.getItem(4).getItemMeta().getLore().stream()
                .anyMatch(line -> line.endsWith("0/" + capacity)));

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            assertNotNull(inventory.getItem(slot), "Slot " + slot + " darf nicht leer sein");
        }
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(0).getType());
    }

    @Test
    void bannerSelectionUpdatesAllOpenMenusAndMarksOwnFaction() {
        FactionSelectionService selectionService = new FactionSelectionService();
        FactionSelectionMenu menu = menu(IsleWarsMode.FOUR_BY_TWO, selectionService);
        PlayerMock secondPlayer = this.server.addPlayer();
        menu.open(this.player);
        menu.open(secondPlayer);

        ProtectedInventoryHolder holder = (ProtectedInventoryHolder)
                this.player.getOpenInventory().getTopInventory().getHolder();
        holder.handleClick(this.player, 18, ClickType.LEFT);

        assertEquals(Faction.FORTITUDO,
                selectionService.factionOf(this.player.getUniqueId()).orElseThrow());
        ItemStack ownBanner = this.player.getOpenInventory().getTopInventory().getItem(18);
        ItemStack otherViewBanner = secondPlayer.getOpenInventory().getTopInventory().getItem(18);
        assertFalse(ownBanner.getEnchantments().isEmpty());
        assertTrue(ownBanner.getItemMeta().getLore().stream().anyMatch(line -> line.endsWith("Ausgewählt")));
        assertTrue(otherViewBanner.getItemMeta().getLore().stream().anyMatch(line -> line.endsWith("1/2")));
    }

    private FactionSelectionMenu menu(IsleWarsMode mode, FactionSelectionService selectionService) {
        ServerRuntimeService runtimeService = new ServerRuntimeService();
        runtimeService.applyConfiguration(new ServerConfiguration(
                mode, 1, mode.maximumCapacity(), 60,
                new LobbySpawn("world", 0, 64, 0, 0, 0)), ignored -> true);
        return new FactionSelectionMenu(runtimeService, selectionService);
    }

    private static void assertResourceIsland(Inventory inventory, int slot) {
        assertEquals(Material.TUFF, inventory.getItem(slot).getType());
    }
}
