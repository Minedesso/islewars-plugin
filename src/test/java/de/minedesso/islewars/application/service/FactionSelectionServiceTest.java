package de.minedesso.islewars.application.service;

import de.minedesso.islewars.domain.model.Faction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionSelectionServiceTest {
    private final FactionSelectionService service = new FactionSelectionService();

    @Test
    void selectsAndChangesFactionWhileKeepingCountsCurrent() {
        UUID player = UUID.randomUUID();

        assertEquals(FactionSelectionService.SelectionResult.SELECTED,
                this.service.select(player, Faction.FORTITUDO, 2));
        assertEquals(FactionSelectionService.SelectionResult.ALREADY_SELECTED,
                this.service.select(player, Faction.FORTITUDO, 2));
        assertEquals(FactionSelectionService.SelectionResult.SELECTED,
                this.service.select(player, Faction.SAPIENTIA, 2));

        assertEquals(0, this.service.size(Faction.FORTITUDO));
        assertEquals(1, this.service.size(Faction.SAPIENTIA));
        assertEquals(Faction.SAPIENTIA, this.service.factionOf(player).orElseThrow());
    }

    @Test
    void rejectsFullFactionWithoutLosingCurrentSelection() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        this.service.select(first, Faction.FORTITUDO, 1);
        this.service.select(second, Faction.CELERITAS, 1);

        assertEquals(FactionSelectionService.SelectionResult.FULL,
                this.service.select(second, Faction.FORTITUDO, 1));
        assertEquals(Faction.CELERITAS, this.service.factionOf(second).orElseThrow());
    }

    @Test
    void removalReleasesFactionSlot() {
        UUID player = UUID.randomUUID();
        this.service.select(player, Faction.TENACITAS, 1);

        assertEquals(Faction.TENACITAS, this.service.remove(player).orElseThrow());
        assertFalse(this.service.factionOf(player).isPresent());
        assertEquals(0, this.service.size(Faction.TENACITAS));
    }

    @Test
    void assignsUnselectedPlayersDeterministicallyAndEvenly() {
        UUID selected = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        UUID third = new UUID(0, 3);
        UUID fourth = new UUID(0, 4);
        UUID fifth = new UUID(0, 5);
        this.service.select(selected, Faction.FORTITUDO, 2);

        Map<UUID, Faction> assigned = this.service.assignUnselected(
                List.of(fifth, third, selected, fourth, second), 2);

        assertEquals(Faction.CELERITAS, assigned.get(second));
        assertEquals(Faction.SAPIENTIA, assigned.get(third));
        assertEquals(Faction.TENACITAS, assigned.get(fourth));
        assertEquals(Faction.FORTITUDO, assigned.get(fifth));
        assertFalse(assigned.containsKey(selected));
        assertTrue(this.service.sizes().values().stream().allMatch(size -> size >= 1 && size <= 2));
    }
}
