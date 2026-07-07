package com.jeiqi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PieceType {
    @JsonProperty("king") KING(0, "将/帅"),
    @JsonProperty("rook") CHARIOT(1, "车"),
    @JsonProperty("knight") HORSE(2, "马"),
    @JsonProperty("cannon") CANNON(3, "炮"),
    @JsonProperty("pawn") PAWN(4, "兵/卒"),
    @JsonProperty("guard") ADVISOR(5, "士/仕"),
    @JsonProperty("bishop") ELEPHANT(6, "象/相");

    private final int code;
    private final String displayName;

    PieceType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
