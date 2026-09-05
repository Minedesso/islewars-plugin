package de.minedesso.islewars.application.lobby;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyItemServiceTest {
    private ServerMock server;
    private PlayerMock player;
    private LobbyItemService service;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.service = LobbyItemService.withPlaceholderActions();
        this.service.giveItems(this.player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void givesAllThreeItemsInFixedSlots() {
        assertEquals(Material.COMPASS, this.player.getInventory().getItem(0).getType());
        assertEquals(Material.PLAYER_HEAD, this.player.getInventory().getItem(4).getType());
        assertEquals(Material.SLIME_BALL, this.player.getInventory().getItem(8).getType());
    }

    @Test
    void executesPlaceholderActionOnlyForMatchingSlotAndItem() {
        assertTrue(this.service.executeFor(this.player, 0, this.player.getInventory().getItem(0)));
        assertTrue(this.player.nextMessage().endsWith("Die Map-Auswahl ist noch nicht verfügbar."));

        assertFalse(this.service.executeFor(this.player, 4, this.player.getInventory().getItem(0)));
    }
}
