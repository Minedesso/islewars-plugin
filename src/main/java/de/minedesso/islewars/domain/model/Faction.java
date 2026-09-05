package de.minedesso.islewars.domain.model;

public enum Faction {
    FORTITUDO("Fortitudo", "Stärke"),
    CELERITAS("Celeritas", "Schnelligkeit"),
    SAPIENTIA("Sapientia", "Weisheit"),
    TENACITAS("Tenacitas", "Ausdauer");

    private final String displayName;
    private final String strength;

    Faction(String displayName, String strength) {
        this.displayName = displayName;
        this.strength = strength;
    }

    public String displayName() {
        return this.displayName;
    }

    public String strength() {
        return this.strength;
    }
}
