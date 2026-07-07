package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;

import java.util.ArrayList;
import java.util.List;

public class AdvisorRule implements MoveRule {

    private static final int[][] DIAGONALS = {
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        boolean isRestricted = !piece.isRevealed();
        int minRow = (piece.getSide() == com.jeiqi.model.Side.RED) ? 0 : 7;
        int maxRow = (piece.getSide() == com.jeiqi.model.Side.RED) ? 2 : 9;

        for (int[] dir : DIAGONALS) {
            int col = from.getCol() + dir[0];
            int row = from.getRow() + dir[1];
            if (col < 0 || col >= ChessBoard.COLS || row < 0 || row >= ChessBoard.ROWS) continue;

            if (isRestricted) {
                if (col < 3 || col > 5 || row < minRow || row > maxRow) continue;
            }

            Position to = new Position(col, row);
            ChessPiece target = board.getPieceAt(to);
            if (target == null || target.getSide() != piece.getSide()) {
                moves.add(to);
            }
        }
        return moves;
    }
}
