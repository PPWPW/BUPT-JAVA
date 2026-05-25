package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;

import java.util.ArrayList;
import java.util.List;

public class ElephantRule implements MoveRule {

    private static final int[][] ELEPHANT_MOVES = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
    private static final int[][] EYE_OFFSETS   = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        for (int i = 0; i < ELEPHANT_MOVES.length; i++) {
            int col = from.getCol() + ELEPHANT_MOVES[i][0];
            int row = from.getRow() + ELEPHANT_MOVES[i][1];
            if (col < 0 || col >= ChessBoard.COLS || row < 0 || row >= ChessBoard.ROWS) continue;

            int eyeCol = from.getCol() + EYE_OFFSETS[i][0];
            int eyeRow = from.getRow() + EYE_OFFSETS[i][1];
            if (board.hasPieceAt(new Position(eyeCol, eyeRow))) continue;

            Position to = new Position(col, row);
            ChessPiece target = board.getPieceAt(to);
            if (target == null || target.getSide() != piece.getSide()) {
                moves.add(to);
            }
        }
        return moves;
    }
}
