package de.minedesso.islewars.domain.model;

import java.util.Arrays;

public enum IsleWarsMode {
    FOUR_BY_ONE("4x1"),
    FOUR_BY_TWO("4x2"),
    FOUR_BY_THREE("4x3");

    private final String apiValue;

    IsleWarsMode(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return this.apiValue;
    }

    public static IsleWarsMode fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.apiValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter IsleWars-Modus: " + value));
    }
}
