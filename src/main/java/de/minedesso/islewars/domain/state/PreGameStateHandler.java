package de.minedesso.islewars.domain.state;

import de.minedesso.islewars.domain.model.GameState;

public final class PreGameStateHandler implements GameStateHandler {
    @Override
    public GameState state() {
        return GameState.PRE_GAME;
    }

    @Override
    public boolean lobbyProtectionActive() {
        return true;
    }
}
