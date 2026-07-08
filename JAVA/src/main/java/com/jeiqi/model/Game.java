package com.jeiqi.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;
import com.jeiqi.engine.RandomPieceAssigner;

@Entity
@Table(name = "games")
public class Game {

    @Id
    private String id;

    @Transient
    private Player redPlayer;

    @Transient
    private Player blackPlayer;

    @Transient
    private ChessBoard board;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @Transient
    private Side currentTurn;

    @Transient
    private final List<Move> moveHistory;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "game_id", referencedColumnName = "gameId")
    private GameNotation notation;

    @Transient
    private RandomPieceAssigner pieceAssigner;

    private long gameStartTime;
    private long gameEndTime;

    @Transient
    private int movesWithoutCapture;

    @Transient
    private int repeatedCheckCountRed = 0;

    @Transient
    private int repeatedCheckCountBlack = 0;

    @Transient
    private String drawRequestedBy = null;

    private String redPlayerId;
    private String redPlayerName;
    private String blackPlayerId;
    private String blackPlayerName;

    @Enumerated(EnumType.STRING)
    private Side winner;

    private String resultReason;

    public Game() {
        this.board = new ChessBoard();
        this.moveHistory = new ArrayList<>();
        this.status = GameStatus.WAITING;
        this.currentTurn = Side.RED;
        this.movesWithoutCapture = 0;
        this.repeatedCheckCountRed = 0;
        this.repeatedCheckCountBlack = 0;
        this.drawRequestedBy = null;
        this.pieceAssigner = new RandomPieceAssigner();
    }

    public Game(String id) {
        this();
        this.id = id;
    }

    public void start() {
        board.initialize();
        status = GameStatus.PLAYING;
        gameStartTime = System.currentTimeMillis();
        currentTurn = Side.RED;
        notation = new GameNotation(id);
        if (redPlayer != null) {
            notation.setRedPlayerName(redPlayer.getName());
        }
        if (blackPlayer != null) {
            notation.setBlackPlayerName(blackPlayer.getName());
        }
    }

    public void switchTurn() {
        currentTurn = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
    }

    public void addMove(Move move) {
        moveHistory.add(move);
        if (notation != null) {
            notation.addMove(move);
        }
    }

    public Player getCurrentPlayer() {
        return (currentTurn == Side.RED) ? redPlayer : blackPlayer;
    }

    public boolean isTimeout(long currentTime, long timeoutMs, long delayToleranceMs) {
        if (status != GameStatus.PLAYING) {
            return false;
        }
        if (moveHistory.isEmpty()) {
            return (currentTime - gameStartTime) > (timeoutMs + delayToleranceMs);
        }
        Move lastMove = moveHistory.get(moveHistory.size() - 1);
        long elapsed = currentTime - lastMove.getServerReceiveTime();
        return elapsed > (timeoutMs + delayToleranceMs);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Player getRedPlayer() {
        return redPlayer;
    }

    public void setRedPlayer(Player redPlayer) {
        this.redPlayer = redPlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Side getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Side currentTurn) {
        this.currentTurn = currentTurn;
    }

    public List<Move> getMoveHistory() {
        return moveHistory;
    }

    public GameNotation getNotation() {
        return notation;
    }

    public void setNotation(GameNotation notation) {
        this.notation = notation;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public int getMovesWithoutCapture() {
        return movesWithoutCapture;
    }

    public void setMovesWithoutCapture(int movesWithoutCapture) {
        this.movesWithoutCapture = movesWithoutCapture;
    }

    public void incrementMovesWithoutCapture() {
        this.movesWithoutCapture++;
    }

    public void resetMovesWithoutCapture() {
        this.movesWithoutCapture = 0;
    }

    public int getRepeatedCheckCount(Side side) {
        return side == Side.RED ? repeatedCheckCountRed : repeatedCheckCountBlack;
    }

    public void incrementRepeatedCheckCount(Side side) {
        if (side == Side.RED) {
            repeatedCheckCountRed++;
        } else {
            repeatedCheckCountBlack++;
        }
    }

    public void resetRepeatedCheckCount(Side side) {
        if (side == Side.RED) {
            repeatedCheckCountRed = 0;
        } else {
            repeatedCheckCountBlack = 0;
        }
    }

    public String getRedPlayerId() { return redPlayerId; }
    public void setRedPlayerId(String redPlayerId) { this.redPlayerId = redPlayerId; }

    public String getRedPlayerName() { return redPlayerName; }
    public void setRedPlayerName(String redPlayerName) { this.redPlayerName = redPlayerName; }

    public String getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(String blackPlayerId) { this.blackPlayerId = blackPlayerId; }

    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }

    public Side getWinner() { return winner; }
    public void setWinner(Side winner) { this.winner = winner; }

    public String getResultReason() { return resultReason; }
    public void setResultReason(String resultReason) { this.resultReason = resultReason; }

    public long getGameEndTime() { return gameEndTime; }
    public void setGameEndTime(long gameEndTime) { this.gameEndTime = gameEndTime; }

    public String getDrawRequestedBy() { return drawRequestedBy; }
    public void setDrawRequestedBy(String drawRequestedBy) { this.drawRequestedBy = drawRequestedBy; }

    public RandomPieceAssigner getPieceAssigner() { return pieceAssigner; }
    public void setPieceAssigner(RandomPieceAssigner pieceAssigner) { this.pieceAssigner = pieceAssigner; }

    @Override
    public String toString() {
        return "Game{id='" + id + "', status=" + status + ", turn=" + currentTurn + "}";
    }
}
