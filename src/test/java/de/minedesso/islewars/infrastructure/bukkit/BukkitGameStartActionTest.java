package de.minedesso.islewars.infrastructure.bukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.minedesso.islewars.application.lobby.FactionSelectionMenu;
import de.minedesso.islewars.application.lobby.ProtectedInventoryHolder;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.Faction;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitGameStartActionTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void distributesRemainingPlayersClosesMenusAndBroadcastsStart() {
        PlayerMock selectedPlayer = this.server.addPlayer();
        PlayerMock firstUnselected = this.server.addPlayer();
        PlayerMock secondUnselected = this.server.addPlayer();
        ServerRuntimeService runtimeService = readyRuntime();
        FactionSelectionService selectionService = new FactionSelectionService();
        selectionService.select(selectedPlayer.getUniqueId(), Faction.FORTITUDO, 2);
        FactionSelectionMenu menu = new FactionSelectionMenu(runtimeService, selectionService);
        List<PlayerMock> players = List.of(selectedPlayer, firstUnselected, secondUnselected);
        players.forEach(menu::open);

        new BukkitGameStartAction(runtimeService, selectionService, menu).startGame();

        assertEquals(Faction.FORTITUDO,
                selectionService.factionOf(selectedPlayer.getUniqueId()).orElseThrow());
        assertTrue(selectionService.factionOf(firstUnselected.getUniqueId()).isPresent());
        assertTrue(selectionService.factionOf(secondUnselected.getUniqueId()).isPresent());
        assertTrue(firstUnselected.nextMessage().contains("automatisch der Fraktion"));
        assertTrue(secondUnselected.nextMessage().contains("automatisch der Fraktion"));
        assertTrue(selectedPlayer.nextMessage().endsWith("Das Spiel startet jetzt!"));
        players.forEach(player -> {
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            assertFalse(topInventory != null && topInventory.getHolder() instanceof ProtectedInventoryHolder);
        });
    }

    private static ServerRuntimeService readyRuntime() {
        ServerRuntimeService runtimeService = new ServerRuntimeService();
        runtimeService.applyConfiguration(new ServerConfiguration(
                IsleWarsMode.FOUR_BY_TWO, 2, 8, 60,
                new LobbySpawn("world", 0, 64, 0, 0, 0)), ignored -> true);
        return runtimeService;
    }
}
