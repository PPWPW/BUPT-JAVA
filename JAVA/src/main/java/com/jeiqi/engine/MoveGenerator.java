package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.PieceType;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MoveGenerator {

    private final Map<PieceType, MoveRule> revealedRules;
    private final MoveRule hiddenRule;

    public MoveGenerator() {
        this.hiddenRule = new HiddenPieceRule();
        this.revealedRules = new EnumMap<>(PieceType.class);
        revealedRules.put(PieceType.KING, new KingRule());
        revealedRules.put(PieceType.CHARIOT, new ChariotRule());
        revealedRules.put(PieceType.HORSE, new HorseRule());
        revealedRules.put(PieceType.CANNON, new CannonRule());
        revealedRules.put(PieceType.PAWN, new PawnRule());
        revealedRules.put(PieceType.ADVISOR, new AdvisorRule());
        revealedRules.put(PieceType.ELEPHANT, new ElephantRule());
    }

    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null || !piece.isAlive()) return Collections.emptyList();

        MoveRule rule;
        if (piece.isRevealed()) {
            rule = revealedRules.get(piece.getType());
        } else {
            rule = hiddenRule;
        }

        if (rule == null) return Collections.emptyList();
        return rule.getLegalMoves(board, from);
    }

    public List<MoveCandidate> getAllLegalMoves(ChessBoard board, Side side) {
        List<MoveCandidate> allMoves = new ArrayList<>();
        List<ChessPiece> pieces = (side == Side.RED)
            ? new ArrayList<>(board.getRedPieces())
            : new ArrayList<>(board.getBlackPieces());

        for (ChessPiece piece : pieces) {
            if (!piece.isAlive()) continue;
            List<Position> targets = getLegalMoves(board, piece.getPosition());
            for (Position target : targets) {
                allMoves.add(new MoveCandidate(piece.getPosition(), target));
            }
        }
        return allMoves;
    }

    public boolean hasAnyLegalMove(ChessBoard board, Side side) {
        return !getAllLegalMoves(board, side).isEmpty();
    }

    public record MoveCandidate(Position from, Position to) {}
}
