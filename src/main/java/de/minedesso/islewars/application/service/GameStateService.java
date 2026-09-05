package de.minedesso.islewars.application.service;

import de.minedesso.islewars.domain.model.GameState;
import de.minedesso.islewars.domain.state.GameStateHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class GameStateService {
    private final Map<GameState, GameStateHandler> handlers = new EnumMap<>(GameState.class);
    private GameState currentState;

    public GameStateService(Iterable<GameStateHandler> handlers, GameState initialState) {
        for (GameStateHandler handler : handlers) {
            GameStateHandler previous = this.handlers.put(handler.state(), handler);
            if (previous != null) {
                throw new IllegalArgumentException("Mehrere Handler für State " + handler.state());
            }
        }
        this.currentState = Objects.requireNonNull(initialState, "initialState");
        this.requireHandler(initialState).onEnter();
    }

    public GameState currentState() {
        return this.currentState;
    }

    public boolean isLobbyProtectionActive() {
        return this.requireHandler(this.currentState).lobbyProtectionActive();
    }

    public void transitionTo(GameState nextState) {
        Objects.requireNonNull(nextState, "nextState");
        if (this.currentState == nextState) {
            return;
        }
        GameStateHandler nextHandler = this.requireHandler(nextState);
        this.requireHandler(this.currentState).onExit();
        this.currentState = nextState;
        nextHandler.onEnter();
    }

    private GameStateHandler requireHandler(GameState state) {
        GameStateHandler handler = this.handlers.get(state);
        if (handler == null) {
            throw new IllegalStateException("Kein Handler für State " + state + " registriert.");
        }
        return handler;
    }
}
