package com.jeiqi.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "notations")
public class GameNotation {

    @Id
    private String gameId;

    private String redPlayerName;
    private String blackPlayerName;

    @ElementCollection
    @CollectionTable(name = "notation_moves", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "move_order")
    private List<Move> moves;

    private String result;
    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date gameDate;

    public GameNotation() {
        this.moves = new ArrayList<>();
        this.gameDate = new Date();
    }

    public GameNotation(String gameId) {
        this();
        this.gameId = gameId;
    }

    public void addMove(Move move) {
        move.setMoveNumber(moves.size() + 1);
        moves.add(move);
    }

    public String exportToText() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Game \"").append(gameId).append("\"]\n");
        if (redPlayerName != null) {
            sb.append("[Red \"").append(redPlayerName).append("\"]\n");
        }
        if (blackPlayerName != null) {
            sb.append("[Black \"").append(blackPlayerName).append("\"]\n");
        }
        sb.append("[Result \"").append(result != null ? result : "*").append("\"]\n");
        sb.append("[Date \"").append(String.format("%tF", gameDate)).append("\"]\n");
        if (reason != null) {
            sb.append("[Reason \"").append(reason).append("\"]\n");
        }
        sb.append("\n");
        for (Move m : moves) {
            sb.append(m.toString()).append("\n");
        }
        return sb.toString();
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getRedPlayerName() {
        return redPlayerName;
    }

    public void setRedPlayerName(String redPlayerName) {
        this.redPlayerName = redPlayerName;
    }

    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    public void setBlackPlayerName(String blackPlayerName) {
        this.blackPlayerName = blackPlayerName;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getGameDate() {
        return gameDate;
    }

    public void setGameDate(Date gameDate) {
        this.gameDate = gameDate;
    }

    public int getMoveCount() {
        return moves.size();
    }
}
