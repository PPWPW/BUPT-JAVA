package com.jeiqi.model;

public class GameResult {

    public enum EndReason {
        CHECKMATE, STALEMATE, KING_CAPTURED, TIMEOUT, RESIGN,
        NO_CAPTURE_DRAW, PERPETUAL_CHECK, DRAW_AGREED, DISCONNECT
    }

    private final boolean draw;
    private final Side winner;
    private final EndReason reason;

    private GameResult(boolean draw, Side winner, EndReason reason) {
        this.draw = draw;
        this.winner = winner;
        this.reason = reason;
    }

    public static GameResult win(Side winner, EndReason reason) {
        return new GameResult(false, winner, reason);
    }

    public static GameResult draw(EndReason reason) {
        return new GameResult(true, null, reason);
    }

    public boolean isDraw() { return draw; }
    public Side getWinner() { return winner; }
    public EndReason getReason() { return reason; }

    @Override
    public String toString() {
        if (draw) return "DRAW (" + reason + ")";
        return winner + " wins (" + reason + ")";
    }
}
