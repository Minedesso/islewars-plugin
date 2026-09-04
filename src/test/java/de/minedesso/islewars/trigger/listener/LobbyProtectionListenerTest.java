package de.minedesso.islewars.trigger.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.minedesso.islewars.application.lobby.LobbyItemService;
import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.service.GameStateService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.GameState;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import de.minedesso.islewars.domain.state.PreGameStateHandler;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyProtectionListenerTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private LobbyProtectionListener listener;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.player = this.server.addPlayer();

        ServerRuntimeService runtimeService = new ServerRuntimeService();
        runtimeService.applyConfiguration(new ServerConfiguration(
                IsleWarsMode.FOUR_BY_TWO,
                2,
                8,
                60,
                new LobbySpawn("world", 0, 64, 0, 0, 0)
        ), ignored -> true);
        LobbyItemService itemService = LobbyItemService.withPlaceholderActions();
        LobbyPlayerService playerService = new LobbyPlayerService(runtimeService, itemService);
        itemService.giveItems(this.player);

        GameStateService gameStateService = new GameStateService(
                List.of(new PreGameStateHandler()), GameState.PRE_GAME);
        this.listener = new LobbyProtectionListener(gameStateService, itemService, playerService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void rightClickInAirExecutesItemActionAndCancelsInteraction() {
        PlayerInteractEvent event = new PlayerInteractEvent(
                this.player,
                Action.RIGHT_CLICK_AIR,
                this.player.getInventory().getItem(0),
                null,
                null,
                EquipmentSlot.HAND
        );

        this.listener.onInteract(event);

        assertTrue(event.isCancelled());
        assertTrue(this.player.nextMessage().endsWith("Die Map-Auswahl ist noch nicht verfügbar."));
    }

    @Test
    void rightClickOnBlockExecutesItemActionAndCancelsInteraction() {
        PlayerInteractEvent event = new PlayerInteractEvent(
                this.player,
                Action.RIGHT_CLICK_BLOCK,
                this.player.getInventory().getItem(0),
                this.world.getBlockAt(0, 63, 0),
                BlockFace.UP,
                EquipmentSlot.HAND
        );

        this.listener.onInteract(event);

        assertTrue(event.isCancelled());
        assertTrue(this.player.nextMessage().endsWith("Die Map-Auswahl ist noch nicht verfügbar."));
    }
}
