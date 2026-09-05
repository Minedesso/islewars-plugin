package de.minedesso.islewars.trigger.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.minedesso.islewars.application.lobby.FactionSelectionMenu;
import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.service.CountdownService;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.Faction;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerLifecycleListenerTest {
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        this.player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void quitReleasesFactionAndRefreshesOpenMenus() {
        ServerRuntimeService runtimeService = new ServerRuntimeService();
        FactionSelectionService selectionService = new FactionSelectionService();
        selectionService.select(this.player.getUniqueId(), Faction.FORTITUDO, 2);
        FactionSelectionMenu menu = mock(FactionSelectionMenu.class);
        PlayerLifecycleListener listener = new PlayerLifecycleListener(
                runtimeService,
                mock(LobbyPlayerService.class),
                mock(CountdownService.class),
                selectionService,
                menu
        );

        listener.onQuit(new PlayerQuitEvent(this.player, "quit"));

        assertTrue(selectionService.factionOf(this.player.getUniqueId()).isEmpty());
        verify(menu).refreshOpenMenus();
    }
}
