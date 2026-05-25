package com.jeiqi.model;

public class MoveResult {

    private final boolean valid;
    private final String errorMessage;
    private final boolean captured;
    private final PieceType revealedType;
    private final boolean gameOver;
    private final GameResult gameResult;

    private MoveResult(boolean valid, String errorMessage, boolean captured,
                       PieceType revealedType, boolean gameOver, GameResult gameResult) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.captured = captured;
        this.revealedType = revealedType;
        this.gameOver = gameOver;
        this.gameResult = gameResult;
    }

    public static MoveResult invalid(String message) {
        return new MoveResult(false, message, false, null, false, null);
    }

    public static MoveResult success(boolean captured, PieceType revealedType) {
        return new MoveResult(true, null, captured, revealedType, false, null);
    }

    public static MoveResult gameOver(boolean captured, PieceType revealedType, GameResult result) {
        return new MoveResult(true, null, captured, revealedType, true, result);
    }

    public boolean isValid() { return valid; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isCaptured() { return captured; }
    public PieceType getRevealedType() { return revealedType; }
    public boolean isGameOver() { return gameOver; }
    public GameResult getGameResult() { return gameResult; }
}
