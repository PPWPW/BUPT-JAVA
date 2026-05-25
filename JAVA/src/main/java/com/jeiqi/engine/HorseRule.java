package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;

import java.util.ArrayList;
import java.util.List;

public class HorseRule implements MoveRule {

    private static final int[][] HORSE_MOVES = {
        {-1, -2}, {1, -2}, {-1, 2}, {1, 2},
        {-2, -1}, {-2, 1}, {2, -1}, {2, 1}
    };

    private static final int[][] LEG_BLOCKS = {
        {0, -1}, {0, -1}, {0, 1}, {0, 1},
        {-1, 0}, {-1, 0}, {1, 0}, {1, 0}
    };

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        for (int i = 0; i < HORSE_MOVES.length; i++) {
            int col = from.getCol() + HORSE_MOVES[i][0];
            int row = from.getRow() + HORSE_MOVES[i][1];
            if (col < 0 || col >= ChessBoard.COLS || row < 0 || row >= ChessBoard.ROWS) continue;

            int legCol = from.getCol() + LEG_BLOCKS[i][0];
            int legRow = from.getRow() + LEG_BLOCKS[i][1];
            if (board.hasPieceAt(new Position(legCol, legRow))) continue;

            Position to = new Position(col, row);
            ChessPiece target = board.getPieceAt(to);
            if (target == null || target.getSide() != piece.getSide()) {
                moves.add(to);
            }
        }
        return moves;
    }
}
