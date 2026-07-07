package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.GameStatus;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.PieceType;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

public class GameFlow {

    private final RuleEngine ruleEngine;
    private final RandomPieceAssigner pieceAssigner;

    public GameFlow(RuleEngine ruleEngine, RandomPieceAssigner pieceAssigner) {
        this.ruleEngine = ruleEngine;
        this.pieceAssigner = pieceAssigner;
    }

    public MoveResult executeMove(Game game, Move move) {
        if (game.getStatus() == GameStatus.FINISHED) {
            return MoveResult.invalid("对局已结束");
        }

        MoveResult validation = ruleEngine.validateMove(game, move);
        if (!validation.isValid()) {
            return validation;
        }

        ChessBoard board = game.getBoard();
        Position from = Position.fromAlgebraic(move.getSource());
        Position to = Position.fromAlgebraic(move.getDestination());
        ChessPiece piece = board.getPieceAt(from);

        boolean captured = false;
        PieceType revealedType = null;

        ChessPiece target = board.getPieceAt(to);
        if (target != null) {
            captured = true;
            if (!target.isRevealed()) {
                target.setCapturedAsHidden(true);
                PieceType capturedType = pieceAssigner.assignType(target.getSide());
                target.reveal(capturedType);
            }
            game.resetMovesWithoutCapture();
        } else {
            game.incrementMovesWithoutCapture();
        }

        board.movePiece(from, to);

        if (!piece.isRevealed()) {
            revealedType = pieceAssigner.assignType(piece.getSide());
            board.revealPiece(to, revealedType);
        }

        // Determine if this move is a check or catch
        Side movingSide = piece.getSide();
        Side opponentSide = (movingSide == Side.RED) ? Side.BLACK : Side.RED;
        boolean isCheck = ruleEngine.isInCheck(board, opponentSide);
        boolean isCatch = false;
        
        if (!isCheck && piece.getType() != PieceType.PAWN) {
            java.util.List<Position> attacks = ruleEngine.getMoveGenerator().getLegalMoves(board, from.equals(to) ? from : to);
            for (Position attackPos : attacks) {
                ChessPiece attackedPiece = board.getPieceAt(attackPos);
                if (attackedPiece != null && attackedPiece.getSide() == opponentSide && attackedPiece.isAlive() && !attackedPiece.isKing()) {
                    isCatch = true;
                    break;
                }
            }
        }

        if (isCheck || isCatch) {
            game.incrementRepeatedCheckCount(movingSide);
        } else {
            game.resetRepeatedCheckCount(movingSide);
        }

        move.setType(revealedType != null ? revealedType.getCode() : null);
        move.setTurnStartTime(System.currentTimeMillis());
        move.setServerReceiveTime(System.currentTimeMillis());
        game.addMove(move);

        GameResult gameResult = ruleEngine.checkGameOver(game);
        if (gameResult != null) {
            game.setStatus(GameStatus.FINISHED);
            if (game.getNotation() != null) {
                game.getNotation().setResult(gameResult.isDraw() ? "1/2-1/2"
                    : (gameResult.getWinner() == Side.RED ? "1-0" : "0-1"));
                game.getNotation().setReason(gameResult.getReason().name());
            }
            return MoveResult.gameOver(captured, revealedType, gameResult);
        }

        game.switchTurn();
        return MoveResult.success(captured, revealedType);
    }
}
