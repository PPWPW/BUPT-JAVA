package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;

import java.util.ArrayList;
import java.util.List;

public class CannonRule implements MoveRule {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        for (int[] dir : DIRECTIONS) {
            int col = from.getCol();
            int row = from.getRow();
            boolean foundMount = false;

            while (true) {
                col += dir[0];
                row += dir[1];
                if (col < 0 || col >= ChessBoard.COLS || row < 0 || row >= ChessBoard.ROWS) break;
                Position to = new Position(col, row);
                ChessPiece target = board.getPieceAt(to);

                if (!foundMount) {
                    if (target == null) {
                        moves.add(to);
                    } else {
                        foundMount = true;
                    }
                } else {
                    if (target != null) {
                        if (target.getSide() != piece.getSide()) {
                            moves.add(to);
                        }
                        break;
                    }
                }
            }
        }
        return moves;
    }
}
