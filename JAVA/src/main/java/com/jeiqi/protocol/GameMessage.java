package com.jeiqi.protocol;

import java.util.Map;

public class GameMessage {

    private MessageType type;
    private String gameId;
    private String playerId;
    private long timestamp;
    private Map<String, Object> payload;

    public GameMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public GameMessage(MessageType type, String gameId, String playerId, Map<String, Object> payload) {
        this.type = type;
        this.gameId = gameId;
        this.playerId = playerId;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
