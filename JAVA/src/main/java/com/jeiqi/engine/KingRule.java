package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import java.util.ArrayList;
import java.util.List;

public class KingRule implements MoveRule {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        for (int[] dir : DIRECTIONS) {
            int col = from.getCol() + dir[0];
            int row = from.getRow() + dir[1];
            if (!inPalace(col, row, piece.getSide())) continue;
            Position to = new Position(col, row);
            ChessPiece target = board.getPieceAt(to);
            if (target == null || target.getSide() != piece.getSide()) {
                moves.add(to);
            }
        }
        return moves;
    }

    private boolean inPalace(int col, int row, Side side) {
        if (col < 3 || col > 5) return false;
        if (side == Side.RED) return row >= 0 && row <= 2;
        else return row >= 7 && row <= 9;
    }
}
