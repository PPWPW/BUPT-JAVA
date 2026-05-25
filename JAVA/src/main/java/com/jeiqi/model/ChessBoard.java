package com.jeiqi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChessBoard {

    public static final int ROWS = 10;
    public static final int COLS = 9;

    private static final int[][] RED_NON_KING_POSITIONS = {
        {0, 0}, {1, 0}, {2, 0}, {3, 0}, {5, 0}, {6, 0}, {7, 0}, {8, 0},
        {1, 2}, {7, 2},
        {0, 3}, {2, 3}, {4, 3}, {6, 3}, {8, 3}
    };

    private static final int[][] BLACK_NON_KING_POSITIONS = {
        {0, 9}, {1, 9}, {2, 9}, {3, 9}, {5, 9}, {6, 9}, {7, 9}, {8, 9},
        {1, 7}, {7, 7},
        {0, 6}, {2, 6}, {4, 6}, {6, 6}, {8, 6}
    };

    private static final Position RED_KING_POS = new Position(4, 0);
    private static final Position BLACK_KING_POS = new Position(4, 9);

    private final ChessPiece[][] grid;
    private final List<ChessPiece> redPieces;
    private final List<ChessPiece> blackPieces;
    private final List<ChessPiece> capturedPieces;

    public ChessBoard() {
        this.grid = new ChessPiece[ROWS][COLS];
        this.redPieces = new ArrayList<>();
        this.blackPieces = new ArrayList<>();
        this.capturedPieces = new ArrayList<>();
    }

    public void initialize() {
        clear();

        grid[RED_KING_POS.getRow()][RED_KING_POS.getCol()] =
            new ChessPiece(PieceType.KING, Side.RED, RED_KING_POS, true);
        redPieces.add(grid[RED_KING_POS.getRow()][RED_KING_POS.getCol()]);

        grid[BLACK_KING_POS.getRow()][BLACK_KING_POS.getCol()] =
            new ChessPiece(PieceType.KING, Side.BLACK, BLACK_KING_POS, true);
        blackPieces.add(grid[BLACK_KING_POS.getRow()][BLACK_KING_POS.getCol()]);

        for (int[] pos : RED_NON_KING_POSITIONS) {
            Position position = new Position(pos[0], pos[1]);
            ChessPiece piece = new ChessPiece(null, Side.RED, position, false);
            grid[pos[1]][pos[0]] = piece;
            redPieces.add(piece);
        }

        for (int[] pos : BLACK_NON_KING_POSITIONS) {
            Position position = new Position(pos[0], pos[1]);
            ChessPiece piece = new ChessPiece(null, Side.BLACK, position, false);
            grid[pos[1]][pos[0]] = piece;
            blackPieces.add(piece);
        }
    }

    private void clear() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = null;
            }
        }
        redPieces.clear();
        blackPieces.clear();
        capturedPieces.clear();
    }

    public ChessPiece getPieceAt(Position p) {
        if (!isPositionValid(p)) {
            return null;
        }
        return grid[p.getRow()][p.getCol()];
    }

    public ChessPiece getPieceAt(int col, int row) {
        return getPieceAt(new Position(col, row));
    }

    public void movePiece(Position from, Position to) {
        ChessPiece piece = getPieceAt(from);
        if (piece == null) {
            throw new IllegalArgumentException("No piece at " + from);
        }

        ChessPiece target = getPieceAt(to);
        if (target != null) {
            target.capture();
            capturedPieces.add(target);
            if (target.getSide() == Side.RED) {
                redPieces.remove(target);
            } else {
                blackPieces.remove(target);
            }
        }

        grid[from.getRow()][from.getCol()] = null;
        grid[to.getRow()][to.getCol()] = piece;
        piece.setPosition(to);
    }

    public void revealPiece(Position p, PieceType type) {
        ChessPiece piece = getPieceAt(p);
        if (piece != null) {
            piece.reveal(type);
        }
    }

    public boolean isPositionValid(Position p) {
        return p != null && p.getCol() >= 0 && p.getCol() < COLS && p.getRow() >= 0 && p.getRow() < ROWS;
    }

    public boolean hasPieceAt(Position p) {
        return getPieceAt(p) != null;
    }

    public List<ChessPiece> getRedPieces() {
        return Collections.unmodifiableList(redPieces);
    }

    public List<ChessPiece> getBlackPieces() {
        return Collections.unmodifiableList(blackPieces);
    }

    public List<ChessPiece> getCapturedPieces() {
        return Collections.unmodifiableList(capturedPieces);
    }

    public int getRedPieceCount() {
        return redPieces.size();
    }

    public int getBlackPieceCount() {
        return blackPieces.size();
    }

    public void setPieceAt(Position p, ChessPiece piece) {
        grid[p.getRow()][p.getCol()] = piece;
        if (piece != null) {
            piece.setPosition(p);
        }
    }

    public void addToSideList(ChessPiece piece) {
        if (piece.getSide() == Side.RED) {
            redPieces.add(piece);
        } else {
            blackPieces.add(piece);
        }
    }

    public void removeFromSideList(ChessPiece piece) {
        if (piece.getSide() == Side.RED) {
            redPieces.remove(piece);
        } else {
            blackPieces.remove(piece);
        }
    }

    public ChessPiece[][] getGrid() {
        return grid;
    }
}
