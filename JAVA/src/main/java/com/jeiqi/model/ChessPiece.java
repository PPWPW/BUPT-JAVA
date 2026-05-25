package com.jeiqi.model;

import java.util.Objects;

public class ChessPiece {

    private PieceType type;
    private final Side side;
    private boolean revealed;
    private Position position;
    private boolean alive;

    public ChessPiece(PieceType type, Side side, Position position, boolean revealed) {
        this.type = type;
        this.side = side;
        this.position = position;
        this.revealed = revealed;
        this.alive = true;
    }

    public MoveRule getMoveRule() {
        return null;
    }

    public void reveal(PieceType actualType) {
        if (this.revealed) {
            return;
        }
        this.type = actualType;
        this.revealed = true;
    }

    public void capture() {
        this.alive = false;
        this.position = null;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public PieceType getType() {
        return type;
    }

    public Side getSide() {
        return side;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public boolean isAlive() {
        return alive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPiece that)) return false;
        return type == that.type
            && side == that.side
            && revealed == that.revealed
            && alive == that.alive
            && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, side, revealed, position, alive);
    }

    public boolean isKing() {
        return revealed && type == PieceType.KING;
    }

    @Override
    public String toString() {
        String display = revealed && type != null ? type.name() : "HIDDEN";
        return side + " " + display + "@" + position;
    }
}
