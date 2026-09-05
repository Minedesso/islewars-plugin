package de.minedesso.islewars.domain.state;

import de.minedesso.islewars.domain.model.GameState;

public interface GameStateHandler {
    GameState state();

    default void onEnter() {
    }

    default void onExit() {
    }

    boolean lobbyProtectionActive();
}
