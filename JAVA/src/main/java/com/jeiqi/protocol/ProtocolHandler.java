package com.jeiqi.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProtocolHandler {

    private final ObjectMapper objectMapper;

    public ProtocolHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(GameMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to encode message", e);
        }
    }

    public GameMessage decode(String json) {
        try {
            return objectMapper.readValue(json, GameMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to decode message", e);
        }
    }
}
