package com.jeiqi.model;

import java.util.List;

public interface MoveRule {
    List<Position> getLegalMoves(ChessBoard board, Position from);
}
