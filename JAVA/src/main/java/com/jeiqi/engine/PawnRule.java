package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import java.util.ArrayList;
import java.util.List;

public class PawnRule implements MoveRule {

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        List<Position> moves = new ArrayList<>();
        ChessPiece piece = board.getPieceAt(from);
        if (piece == null) return moves;

        int forward = (piece.getSide() == Side.RED) ? 1 : -1;
        int row = from.getRow();
        int col = from.getCol();

        addMoveIfValid(board, moves, piece, col, row + forward);

        if (hasCrossedRiver(row, piece.getSide())) {
            addMoveIfValid(board, moves, piece, col - 1, row);
            addMoveIfValid(board, moves, piece, col + 1, row);
        }

        return moves;
    }

    private void addMoveIfValid(ChessBoard board, List<Position> moves, ChessPiece piece, int col, int row) {
        if (col < 0 || col >= ChessBoard.COLS || row < 0 || row >= ChessBoard.ROWS) return;
        Position to = new Position(col, row);
        ChessPiece target = board.getPieceAt(to);
        if (target == null || target.getSide() != piece.getSide()) {
            moves.add(to);
        }
    }

    private boolean hasCrossedRiver(int row, Side side) {
        return side == Side.RED ? row >= 5 : row <= 4;
    }
}
