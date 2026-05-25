package com.jeiqi.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Move {

    private String source;
    private String destination;
    private Integer type;
    private long turnStartTime;
    private long serverReceiveTime;
    private int moveNumber;
    @Enumerated(EnumType.STRING)
    private Side side;
    private boolean revealMove;

    public Move() {
    }

    public Move(String source, String destination, Integer type, Side side) {
        this.source = source;
        this.destination = destination;
        this.type = type;
        this.side = side;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public long getTurnStartTime() {
        return turnStartTime;
    }

    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    public long getServerReceiveTime() {
        return serverReceiveTime;
    }

    public void setServerReceiveTime(long serverReceiveTime) {
        this.serverReceiveTime = serverReceiveTime;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public boolean isRevealMove() {
        return revealMove;
    }

    public void setRevealMove(boolean revealMove) {
        this.revealMove = revealMove;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(moveNumber).append(". ").append(source).append(destination);
        if (type != null) {
            sb.append(",").append(PieceType.values()[type].name().charAt(0));
        }
        return sb.toString();
    }
}
