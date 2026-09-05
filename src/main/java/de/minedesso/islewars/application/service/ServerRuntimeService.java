package de.minedesso.islewars.application.service;

import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class ServerRuntimeService {
    private ServerConfiguration configuration;
    private LobbySpawn locallySavedSpawn;
    private boolean ready;

    public synchronized boolean applyConfiguration(
            ServerConfiguration loadedConfiguration,
            Predicate<LobbySpawn> spawnAvailable
    ) {
        Objects.requireNonNull(loadedConfiguration, "loadedConfiguration");
        Objects.requireNonNull(spawnAvailable, "spawnAvailable");

        boolean wasReady = this.ready;
        this.configuration = this.locallySavedSpawn == null
                ? loadedConfiguration
                : loadedConfiguration.withLobbySpawn(this.locallySavedSpawn);
        this.ready = this.configuration.optionalLobbySpawn().filter(spawnAvailable).isPresent();
        return !wasReady && this.ready;
    }

    public synchronized boolean updateLobbySpawn(LobbySpawn lobbySpawn, Predicate<LobbySpawn> spawnAvailable) {
        Objects.requireNonNull(lobbySpawn, "lobbySpawn");
        Objects.requireNonNull(spawnAvailable, "spawnAvailable");

        boolean wasReady = this.ready;
        this.locallySavedSpawn = lobbySpawn;
        if (this.configuration != null) {
            this.configuration = this.configuration.withLobbySpawn(lobbySpawn);
        }
        this.ready = this.configuration != null && spawnAvailable.test(lobbySpawn);
        return !wasReady && this.ready;
    }

    public synchronized boolean isReady() {
        return this.ready;
    }

    public synchronized Optional<ServerConfiguration> configuration() {
        return Optional.ofNullable(this.configuration);
    }

    public synchronized ServerConfiguration requireConfiguration() {
        if (!this.ready || this.configuration == null) {
            throw new IllegalStateException("Der IsleWars-Server ist noch nicht bereit.");
        }
        return this.configuration;
    }
}
