package com.jeiqi.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessBoardTest {

    private ChessBoard board;

    @BeforeEach
    void setUp() {
        board = new ChessBoard();
        board.initialize();
    }

    @Test
    void shouldHaveCorrectPieceCountAfterInitialize() {
        assertEquals(16, board.getRedPieceCount(),
            "Red should have 16 pieces (1 king + 15 hidden)");
        assertEquals(16, board.getBlackPieceCount(),
            "Black should have 16 pieces (1 king + 15 hidden)");
    }

    @Test
    void kingsShouldBeRevealed() {
        ChessPiece redKing = board.getPieceAt(new Position(4, 0));
        assertNotNull(redKing, "Red king should be at (4,0)");
        assertEquals(PieceType.KING, redKing.getType());
        assertEquals(Side.RED, redKing.getSide());
        assertTrue(redKing.isRevealed(), "Red king should be revealed");

        ChessPiece blackKing = board.getPieceAt(new Position(4, 9));
        assertNotNull(blackKing, "Black king should be at (4,9)");
        assertEquals(PieceType.KING, blackKing.getType());
        assertEquals(Side.BLACK, blackKing.getSide());
        assertTrue(blackKing.isRevealed(), "Black king should be revealed");
    }

    @Test
    void allNonKingPiecesShouldBeHidden() {
        for (ChessPiece piece : board.getRedPieces()) {
            if (piece.getType() != PieceType.KING) {
                assertFalse(piece.isRevealed(),
                    "Red piece at " + piece.getPosition() + " should be hidden");
            }
        }
        for (ChessPiece piece : board.getBlackPieces()) {
            if (piece.getType() != PieceType.KING) {
                assertFalse(piece.isRevealed(),
                    "Black piece at " + piece.getPosition() + " should be hidden");
            }
        }
    }

    @Test
    void redPiecesShouldBeInRows0To4() {
        for (ChessPiece piece : board.getRedPieces()) {
            int row = piece.getPosition().getRow();
            assertTrue(row >= 0 && row <= 4,
                "Red piece at " + piece.getPosition() + " should be in rows 0-4, got row=" + row);
        }
    }

    @Test
    void blackPiecesShouldBeInRows5To9() {
        for (ChessPiece piece : board.getBlackPieces()) {
            int row = piece.getPosition().getRow();
            assertTrue(row >= 5 && row <= 9,
                "Black piece at " + piece.getPosition() + " should be in rows 5-9, got row=" + row);
        }
    }

    @Test
    void getPieceAtShouldReturnCorrectPiece() {
        ChessPiece piece = board.getPieceAt(new Position(4, 0));
        assertNotNull(piece);
        assertEquals(Side.RED, piece.getSide());
        assertEquals(PieceType.KING, piece.getType());
    }

    @Test
    void getPieceAtShouldReturnNullForEmptyPosition() {
        assertNull(board.getPieceAt(new Position(4, 5)));
    }

    @Test
    void getPieceAtShouldReturnNullForEmptyPositionAtBoundary() {
        assertNull(board.getPieceAt(new Position(0, 5)));
        assertNull(board.getPieceAt(new Position(8, 4)));
    }

    @Test
    void movePieceShouldMovePieceToDestination() {
        Position from = new Position(0, 0);
        Position to = new Position(0, 4);

        ChessPiece piece = board.getPieceAt(from);
        assertNotNull(piece, "Should have a piece at source position");

        board.movePiece(from, to);

        assertNull(board.getPieceAt(from), "Source position should be empty after move");
        assertEquals(piece, board.getPieceAt(to), "Piece should be at destination");
        assertEquals(to, piece.getPosition(), "Piece position should be updated");
    }

    @Test
    void movePieceShouldCaptureEnemyPiece() {
        Position redFrom = new Position(0, 0);
        Position blackPos = new Position(0, 9);
        ChessPiece blackPiece = board.getPieceAt(blackPos);

        board.movePiece(blackPos, new Position(0, 5));
        board.movePiece(redFrom, new Position(0, 5));

        assertEquals(1, board.getCapturedPieces().size(), "Should have captured one piece");
        assertFalse(blackPiece.isAlive(), "Captured piece should not be alive");
        assertNull(blackPiece.getPosition(), "Captured piece should have null position");
    }

    @Test
    void hasPieceAtShouldReturnTrueWhenPieceExists() {
        assertTrue(board.hasPieceAt(new Position(4, 0)));
    }

    @Test
    void hasPieceAtShouldReturnFalseWhenNoPiece() {
        assertFalse(board.hasPieceAt(new Position(4, 5)));
    }

    @Test
    void isPositionValidShouldReturnCorrectResults() {
        assertTrue(board.isPositionValid(new Position(0, 0)));
        assertTrue(board.isPositionValid(new Position(8, 9)));
        assertFalse(board.isPositionValid(null));
    }

    @Test
    void positionShouldRejectInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Position(9, 0));
    }

    @Test
    void revealPieceShouldChangePieceState() {
        Position pos = new Position(0, 0);
        ChessPiece piece = board.getPieceAt(pos);
        assertNotNull(piece);
        assertFalse(piece.isRevealed(), "Non-king piece should start hidden");

        board.revealPiece(pos, PieceType.CHARIOT);

        assertTrue(piece.isRevealed(), "Piece should be revealed after revealPiece");
        assertEquals(PieceType.CHARIOT, piece.getType());
    }

    @Test
    void capturedPiecesShouldBeTracked() {
        Position from = new Position(0, 0);
        Position target = new Position(0, 3);

        board.movePiece(from, target);

        assertEquals(1, board.getCapturedPieces().size());
        ChessPiece captured = board.getCapturedPieces().get(0);
        assertFalse(captured.isAlive());
    }

    @Test
    void initializeShouldPlaceAllPiecesWithoutOverlap() {
        board = new ChessBoard();
        board.initialize();

        ChessPiece[][] grid = board.getGrid();
        int pieceCount = 0;
        for (int r = 0; r < ChessBoard.ROWS; r++) {
            for (int c = 0; c < ChessBoard.COLS; c++) {
                if (grid[r][c] != null) {
                    pieceCount++;
                }
            }
        }
        assertEquals(32, pieceCount, "Grid should contain exactly 32 pieces");
    }
}
