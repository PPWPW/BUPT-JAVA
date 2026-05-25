package com.jeiqi.model;

public enum PieceType {
    KING(0, "将/帅"),
    CHARIOT(1, "车"),
    HORSE(2, "马"),
    CANNON(3, "炮"),
    PAWN(4, "兵/卒"),
    ADVISOR(5, "士/仕"),
    ELEPHANT(6, "象/相");

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
