package de.minedesso.islewars.application.port.out;

public interface LobbyAudience {
    int playerCount();

    void broadcast(String message);
}
