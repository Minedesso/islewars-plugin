package de.minedesso.islewars.application.service;

import de.minedesso.islewars.domain.model.Faction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FactionSelectionService {
    private final Map<UUID, Faction> assignments = new LinkedHashMap<>();

    public synchronized SelectionResult select(UUID playerId, Faction faction, int capacity) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(faction, "faction");
        requireCapacity(capacity);

        Faction current = this.assignments.get(playerId);
        if (faction == current) {
            return SelectionResult.ALREADY_SELECTED;
        }
        if (this.size(faction) >= capacity) {
            return SelectionResult.FULL;
        }
        this.assignments.put(playerId, faction);
        return SelectionResult.SELECTED;
    }

    public synchronized Optional<Faction> factionOf(UUID playerId) {
        return Optional.ofNullable(this.assignments.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public synchronized int size(Faction faction) {
        return (int) this.assignments.values().stream().filter(faction::equals).count();
    }

    public synchronized Map<Faction, Integer> sizes() {
        Map<Faction, Integer> sizes = new EnumMap<>(Faction.class);
        for (Faction faction : Faction.values()) {
            sizes.put(faction, this.size(faction));
        }
        return Map.copyOf(sizes);
    }

    public synchronized Optional<Faction> remove(UUID playerId) {
        return Optional.ofNullable(this.assignments.remove(Objects.requireNonNull(playerId, "playerId")));
    }

    public synchronized Map<UUID, Faction> assignUnselected(Collection<UUID> playerIds, int capacity) {
        Objects.requireNonNull(playerIds, "playerIds");
        requireCapacity(capacity);

        ArrayList<UUID> sortedPlayers = new ArrayList<>(playerIds);
        sortedPlayers.sort(Comparator.naturalOrder());
        Map<UUID, Faction> assigned = new LinkedHashMap<>();

        for (UUID playerId : sortedPlayers) {
            if (this.assignments.containsKey(playerId)) {
                continue;
            }
            Faction target = java.util.Arrays.stream(Faction.values())
                    .filter(faction -> this.size(faction) < capacity)
                    .min(Comparator.comparingInt(this::size).thenComparingInt(Enum::ordinal))
                    .orElseThrow(() -> new IllegalStateException("Keine freie Fraktion für alle Spieler verfügbar."));
            this.assignments.put(playerId, target);
            assigned.put(playerId, target);
        }
        return Map.copyOf(assigned);
    }

    private static void requireCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Die Fraktionskapazität muss mindestens 1 sein.");
        }
    }

    public enum SelectionResult {
        SELECTED,
        ALREADY_SELECTED,
        FULL
    }
}
