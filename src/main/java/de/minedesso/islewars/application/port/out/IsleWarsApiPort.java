package de.minedesso.islewars.application.port.out;

import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;

import java.util.concurrent.CompletableFuture;

public interface IsleWarsApiPort {
    CompletableFuture<ServerConfiguration> fetchServerConfiguration();

    CompletableFuture<Void> saveLobbySpawn(LobbySpawn lobbySpawn);
}
