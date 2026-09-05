package de.minedesso.islewars.domain.model;

import java.util.Arrays;

public enum IsleWarsMode {
    FOUR_BY_ONE("4x1", 1),
    FOUR_BY_TWO("4x2", 2),
    FOUR_BY_THREE("4x3", 3);

    private final String apiValue;
    private final int factionCapacity;

    IsleWarsMode(String apiValue, int factionCapacity) {
        this.apiValue = apiValue;
        this.factionCapacity = factionCapacity;
    }

    public String apiValue() {
        return this.apiValue;
    }

    public int factionCount() {
        return 4;
    }

    public int factionCapacity() {
        return this.factionCapacity;
    }

    public int maximumCapacity() {
        return this.factionCount() * this.factionCapacity;
    }

    public static IsleWarsMode fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.apiValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter IsleWars-Modus: " + value));
    }
}
