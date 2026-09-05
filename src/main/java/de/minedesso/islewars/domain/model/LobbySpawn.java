package de.minedesso.islewars.domain.model;

public record LobbySpawn(
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public LobbySpawn {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("Der Weltname des Lobby-Spawns darf nicht leer sein.");
        }
        worldName = worldName.trim();
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Lobby-Spawn-Feld " + field + " muss endlich sein.");
        }
    }
}
