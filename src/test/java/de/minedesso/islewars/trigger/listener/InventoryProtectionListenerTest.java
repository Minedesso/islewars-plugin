package de.minedesso.islewars.trigger.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.minedesso.islewars.application.lobby.FactionSelectionMenu;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.GameStateService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.Faction;
import de.minedesso.islewars.domain.model.GameState;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import de.minedesso.islewars.domain.state.PreGameStateHandler;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryProtectionListenerTest {
    private ServerMock server;
    private PlayerMock player;
    private FactionSelectionService selectionService;
    private FactionSelectionMenu menu;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.server.addSimpleWorld("world");
        this.player = this.server.addPlayer();
        ServerRuntimeService runtimeService = new ServerRuntimeService();
        runtimeService.applyConfiguration(new ServerConfiguration(
                IsleWarsMode.FOUR_BY_TWO, 2, 8, 60,
                new LobbySpawn("world", 0, 64, 0, 0, 0)), ignored -> true);
        this.selectionService = new FactionSelectionService();
        this.menu = new FactionSelectionMenu(runtimeService, this.selectionService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @EnumSource(value = ClickType.class, names = {"LEFT", "RIGHT"})
    void leftAndRightClickOnBannerSelectFaction(ClickType clickType) {
        this.menu.open(this.player);
        InventoryProtectionListener listener = new InventoryProtectionListener(lobbyState());
        InventoryClickEvent event = click(18, clickType, InventoryAction.PICKUP_ALL);

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(Faction.FORTITUDO,
                this.selectionService.factionOf(this.player.getUniqueId()).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = ClickType.class, names = {"SHIFT_LEFT", "NUMBER_KEY", "DOUBLE_CLICK"})
    void movementClicksAreCancelledWithoutSelectingFaction(ClickType clickType) {
        this.menu.open(this.player);
        InventoryProtectionListener listener = new InventoryProtectionListener(outsideLobbyState());
        InventoryAction action = clickType == ClickType.NUMBER_KEY
                ? InventoryAction.HOTBAR_SWAP
                : InventoryAction.MOVE_TO_OTHER_INVENTORY;
        InventoryClickEvent event = new InventoryClickEvent(
                this.player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                18, clickType, action, clickType == ClickType.NUMBER_KEY ? 1 : -1);

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertTrue(this.selectionService.factionOf(this.player.getUniqueId()).isEmpty());
    }

    @Test
    void protectedMenuBlocksBottomInventoryDragDropAndHandSwapOutsideLobby() {
        this.menu.open(this.player);
        InventoryProtectionListener listener = new InventoryProtectionListener(outsideLobbyState());
        int bottomSlot = this.player.getOpenInventory().getTopInventory().getSize();
        InventoryClickEvent bottomClick = click(bottomSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        ItemStack stone = new ItemStack(Material.STONE);
        InventoryDragEvent drag = new InventoryDragEvent(
                this.player.getOpenInventory(), stone, stone, false, Map.of(0, stone));
        PlayerDropItemEvent drop = new PlayerDropItemEvent(this.player, mock(Item.class));
        PlayerSwapHandItemsEvent swap = new PlayerSwapHandItemsEvent(this.player, stone, stone);

        listener.onInventoryClick(bottomClick);
        listener.onInventoryDrag(drag);
        listener.onDrop(drop);
        listener.onSwapHands(swap);

        assertTrue(bottomClick.isCancelled());
        assertTrue(drag.isCancelled());
        assertTrue(drop.isCancelled());
        assertTrue(swap.isCancelled());
    }

    @Test
    void ownInventoryIsBlockedOnlyInLobby() {
        this.player.openInventory(org.bukkit.Bukkit.createInventory(null, 9, "Normales Inventar"));
        int ownInventorySlot = this.player.getOpenInventory().getTopInventory().getSize();
        InventoryClickEvent lobbyClick = click(ownInventorySlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        InventoryClickEvent gameClick = click(ownInventorySlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        new InventoryProtectionListener(lobbyState()).onInventoryClick(lobbyClick);
        new InventoryProtectionListener(outsideLobbyState()).onInventoryClick(gameClick);

        assertTrue(lobbyClick.isCancelled());
        assertFalse(gameClick.isCancelled());
    }

    private InventoryClickEvent click(int rawSlot, ClickType clickType, InventoryAction action) {
        return new InventoryClickEvent(this.player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                rawSlot, clickType, action);
    }

    private static GameStateService lobbyState() {
        return new GameStateService(List.of(new PreGameStateHandler()), GameState.PRE_GAME);
    }

    private static GameStateService outsideLobbyState() {
        GameStateService service = mock(GameStateService.class);
        when(service.isLobbyProtectionActive()).thenReturn(false);
        return service;
    }
}
