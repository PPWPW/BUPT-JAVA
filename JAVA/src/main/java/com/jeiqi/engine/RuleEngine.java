package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import java.util.List;

public class RuleEngine {

    private final MoveGenerator moveGenerator;

    public RuleEngine(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
    }

    public MoveResult validateMove(Game game, Move move) {
        ChessBoard board = game.getBoard();
        Side currentTurn = game.getCurrentTurn();

        Position from = Position.fromAlgebraic(move.getSource());
        Position to = Position.fromAlgebraic(move.getDestination());

        if (!board.isPositionValid(from)) {
            return MoveResult.invalid("无效的起始位置");
        }

        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) {
            return MoveResult.invalid("起始位置没有棋子");
        }

        if (!piece.isAlive()) {
            return MoveResult.invalid("棋子已被吃掉");
        }

        if (piece.getSide() != currentTurn) {
            return MoveResult.invalid("不是你的回合");
        }

        if (!board.isPositionValid(to)) {
            return MoveResult.invalid("无效的目标位置");
        }

        ChessPiece target = board.getPieceAt(to);

        if (from.equals(to)) {
            if (piece.isRevealed()) {
                return MoveResult.invalid("该棋子已经翻明，不能原地翻面");
            }
            return MoveResult.success(false, null);
        }

        if (target != null && target.getSide() == currentTurn) {
            return MoveResult.invalid("不能吃自己的棋子");
        }

        List<Position> legalMoves = moveGenerator.getLegalMoves(board, from);
        if (!legalMoves.contains(to)) {
            return MoveResult.invalid("不合法的走法");
        }

        return MoveResult.success(target != null, null);
    }

    public GameResult checkGameOver(Game game) {
        ChessBoard board = game.getBoard();

        GameResult kingResult = checkKingCaptured(board);
        if (kingResult != null) return kingResult;

        Side currentTurn = game.getCurrentTurn();

        if (isInCheck(board, currentTurn)) {
            if (!hasEscapeMove(board, currentTurn)) {
                Side winner = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
                return GameResult.win(winner, GameResult.EndReason.CHECKMATE);
            }
        } else {
            if (!moveGenerator.hasAnyLegalMove(board, currentTurn)) {
                Side winner = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
                return GameResult.win(winner, GameResult.EndReason.STALEMATE);
            }
        }

        if (game.getMovesWithoutCapture() >= 40) {
            return GameResult.draw(GameResult.EndReason.NO_CAPTURE_DRAW);
        }

        if (game.getRepeatedCheckCount() >= 6) {
            Side winner = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
            return GameResult.win(winner, GameResult.EndReason.PERPETUAL_CHECK);
        }

        if (isKingsFacing(board)) {
            Side winner = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
            return GameResult.win(winner, GameResult.EndReason.CHECKMATE);
        }

        return null;
    }

    private GameResult checkKingCaptured(ChessBoard board) {
        boolean redKingAlive = false, blackKingAlive = false;
        for (ChessPiece p : board.getRedPieces()) {
            if (p.isKing() && p.isAlive()) { redKingAlive = true; break; }
        }
        for (ChessPiece p : board.getBlackPieces()) {
            if (p.isKing() && p.isAlive()) { blackKingAlive = true; break; }
        }
        if (!redKingAlive) return GameResult.win(Side.BLACK, GameResult.EndReason.KING_CAPTURED);
        if (!blackKingAlive) return GameResult.win(Side.RED, GameResult.EndReason.KING_CAPTURED);
        return null;
    }

    public boolean isInCheck(ChessBoard board, Side side) {
        Position kingPos = findKing(board, side);
        if (kingPos == null) return false;
        Side opponent = (side == Side.RED) ? Side.BLACK : Side.RED;
        return isSquareAttacked(board, kingPos, opponent);
    }

    private boolean hasEscapeMove(ChessBoard board, Side side) {
        List<MoveGenerator.MoveCandidate> allMoves = moveGenerator.getAllLegalMoves(board, side);
        for (MoveGenerator.MoveCandidate mc : allMoves) {
            ChessPiece piece = board.getPieceAt(mc.from());
            ChessPiece captured = board.getPieceAt(mc.to());

            if (captured != null) {
                board.removeFromSideList(captured);
            }
            board.setPieceAt(mc.to(), piece);
            board.setPieceAt(mc.from(), null);

            boolean stillInCheck = isInCheck(board, side);

            board.setPieceAt(mc.from(), piece);
            board.setPieceAt(mc.to(), captured);
            if (captured != null) {
                board.addToSideList(captured);
            }

            if (!stillInCheck) return true;
        }
        return false;
    }

    public boolean isSquareAttacked(ChessBoard board, Position pos, Side attackerSide) {
        List<MoveGenerator.MoveCandidate> allMoves = moveGenerator.getAllLegalMoves(board, attackerSide);
        for (MoveGenerator.MoveCandidate mc : allMoves) {
            if (mc.to().equals(pos)) return true;
        }
        return false;
    }

    private Position findKing(ChessBoard board, Side side) {
        List<ChessPiece> pieces = (side == Side.RED) ? board.getRedPieces() : board.getBlackPieces();
        for (ChessPiece p : pieces) {
            if (p.isKing() && p.isAlive()) return p.getPosition();
        }
        return null;
    }

    private boolean isKingsFacing(ChessBoard board) {
        Position redKing = findKing(board, Side.RED);
        Position blackKing = findKing(board, Side.BLACK);
        if (redKing == null || blackKing == null) return false;
        if (redKing.getCol() != blackKing.getCol()) return false;

        int minRow = Math.min(redKing.getRow(), blackKing.getRow());
        int maxRow = Math.max(redKing.getRow(), blackKing.getRow());
        int col = redKing.getCol();

        for (int r = minRow + 1; r < maxRow; r++) {
            if (board.hasPieceAt(new Position(col, r))) return false;
        }
        return true;
    }
}
