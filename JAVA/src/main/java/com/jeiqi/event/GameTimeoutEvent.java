package com.jeiqi.event;

import com.jeiqi.model.GameResult;
import org.springframework.context.ApplicationEvent;

public class GameTimeoutEvent extends ApplicationEvent {
    private final String gameId;
    private final GameResult result;

    public GameTimeoutEvent(Object source, String gameId, GameResult result) {
        super(source);
        this.gameId = gameId;
        this.result = result;
    }

    public String getGameId() {
        return gameId;
    }

    public GameResult getResult() {
        return result;
    }
}
