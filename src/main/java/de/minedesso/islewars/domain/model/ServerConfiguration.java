package de.minedesso.islewars.domain.model;

import java.util.Objects;
import java.util.Optional;

public record ServerConfiguration(
        IsleWarsMode mode,
        int minimumPlayers,
        int maximumPlayers,
        int countdownSeconds,
        LobbySpawn lobbySpawn
) {
    public ServerConfiguration {
        Objects.requireNonNull(mode, "mode");
        if (minimumPlayers < 1) {
            throw new IllegalArgumentException("minimumPlayers muss mindestens 1 sein.");
        }
        if (maximumPlayers < minimumPlayers) {
            throw new IllegalArgumentException("maximumPlayers darf nicht kleiner als minimumPlayers sein.");
        }
        if (maximumPlayers > mode.maximumCapacity()) {
            throw new IllegalArgumentException("maximumPlayers überschreitet die Kapazität des Modus "
                    + mode.apiValue() + ".");
        }
        if (countdownSeconds < 1) {
            throw new IllegalArgumentException("countdownSeconds muss mindestens 1 sein.");
        }
    }

    public Optional<LobbySpawn> optionalLobbySpawn() {
        return Optional.ofNullable(this.lobbySpawn);
    }

    public ServerConfiguration withLobbySpawn(LobbySpawn spawn) {
        return new ServerConfiguration(this.mode, this.minimumPlayers, this.maximumPlayers,
                this.countdownSeconds, Objects.requireNonNull(spawn, "spawn"));
    }
}
