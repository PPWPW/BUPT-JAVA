package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.PieceType;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveGeneratorTest {

    private MoveGenerator gen;
    private ChessBoard board;

    @BeforeEach
    void setUp() {
        gen = new MoveGenerator();
        board = new ChessBoard();
        board.initialize();
    }

    @Test
    void hiddenPieceShouldMoveOneStepInAllDirections() {
        Position pos = new Position(4, 3);
        List<Position> moves = gen.getLegalMoves(board, pos);
        assertTrue(moves.size() >= 5, "Hidden piece at center should have at least 5 moves");
        for (Position to : moves) {
            int dc = Math.abs(to.getCol() - pos.getCol());
            int dr = Math.abs(to.getRow() - pos.getRow());
            assertTrue(dc <= 1 && dr <= 1 && (dc + dr > 0),
                "Hidden piece should move at most one step, got " + to);
        }
    }

    @Test
    void hiddenPieceAtEdgeShouldHaveFewerMoves() {
        Position pos = new Position(0, 0);
        List<Position> moves = gen.getLegalMoves(board, pos);
        assertFalse(moves.isEmpty(), "Should have some moves even at corner");
        for (Position to : moves) {
            assertTrue(board.isPositionValid(to), "All moves should be valid");
        }
    }

    @Test
    void hiddenPieceShouldNotMoveOntoOwnPiece() {
        Position pos = new Position(4, 2);
        List<Position> moves = gen.getLegalMoves(board, pos);
        for (Position to : moves) {
            ChessPiece target = board.getPieceAt(to);
            if (target != null) {
                assertNotEquals(Side.RED, target.getSide(),
                    "Should not move onto own piece at " + to);
            }
        }
    }

    @Test
    void redKingShouldStayInPalace() {
        Position kingPos = new Position(4, 0);
        ChessPiece king = board.getPieceAt(kingPos);
        king.reveal(PieceType.KING);
        List<Position> moves = gen.getLegalMoves(board, kingPos);
        assertFalse(moves.isEmpty(), "King should have moves");
        for (Position to : moves) {
            assertTrue(to.getCol() >= 3 && to.getCol() <= 5, "King must stay within cols 3-5");
            assertTrue(to.getRow() >= 0 && to.getRow() <= 2, "King must stay within rows 0-2");
        }
    }

    @Test
    void chariotShouldSlideInStraightLines() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.CHARIOT, Side.RED, new Position(4, 5), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(4, 0)), "Chariot should reach row 0 on same column");
        assertTrue(moves.contains(new Position(4, 9)), "Chariot should reach row 9 on same column");
        assertTrue(moves.contains(new Position(0, 5)), "Chariot should reach col 0 on same row");
        assertTrue(moves.contains(new Position(8, 5)), "Chariot should reach col 8 on same row");
    }

    @Test
    void chariotShouldBeBlockedByPiece() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.CHARIOT, Side.RED, new Position(4, 5), true));
        board.setPieceAt(new Position(4, 7),
            new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 7), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(4, 6)), "Should reach row 6");
        assertFalse(moves.contains(new Position(4, 7)), "Should be blocked by own piece at row 7");
        assertFalse(moves.contains(new Position(4, 8)), "Should not reach beyond blocking piece");
    }

    @Test
    void chariotShouldCaptureEnemy() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.CHARIOT, Side.RED, new Position(4, 5), true));
        board.setPieceAt(new Position(4, 7),
            new ChessPiece(PieceType.PAWN, Side.BLACK, new Position(4, 7), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(4, 7)), "Should capture enemy piece at row 7");
        assertFalse(moves.contains(new Position(4, 8)), "Should not reach beyond enemy piece");
    }

    @Test
    void horseShouldMoveInLShape() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.HORSE, Side.RED, new Position(4, 5), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(3, 3)), "Horse L-shape move");
        assertTrue(moves.contains(new Position(5, 3)), "Horse L-shape move");
        assertTrue(moves.contains(new Position(2, 4)), "Horse L-shape move");
        assertTrue(moves.contains(new Position(6, 4)), "Horse L-shape move");
    }

    @Test
    void horseShouldBeBlockedByLegPiece() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.HORSE, Side.RED, new Position(4, 5), true));
        board.setPieceAt(new Position(4, 4),
            new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 4), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertFalse(moves.contains(new Position(3, 3)), "Blocked by leg at (4,4)");
        assertFalse(moves.contains(new Position(5, 3)), "Blocked by leg at (4,4)");
        assertTrue(moves.contains(new Position(2, 4)), "Should still have side moves");
    }

    @Test
    void cannonMovesAndCapturesCorrectly() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.CANNON, Side.RED, new Position(4, 5), true));
        board.setPieceAt(new Position(4, 7),
            new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 7), true));
        board.setPieceAt(new Position(4, 8),
            new ChessPiece(PieceType.PAWN, Side.BLACK, new Position(4, 8), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(4, 6)), "Cannon can move to empty square before mount");
        assertFalse(moves.contains(new Position(4, 7)), "Cannon cannot land on mount (own piece)");
        assertTrue(moves.contains(new Position(4, 8)), "Cannon can capture over mount");
    }

    @Test
    void redPawnBeforeRiverCanOnlyMoveForward() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 3),
            new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 3), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 3));
        assertEquals(1, moves.size(), "Pawn before river should have exactly 1 move");
        assertTrue(moves.contains(new Position(4, 4)), "Pawn should move forward to row 4");
    }

    @Test
    void redPawnAfterCrossingRiverCanMoveThreeWays() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 6),
            new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 6), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 6));
        assertTrue(moves.contains(new Position(4, 7)), "Forward");
        assertTrue(moves.contains(new Position(3, 6)), "Left");
        assertTrue(moves.contains(new Position(5, 6)), "Right");
        assertFalse(moves.contains(new Position(4, 5)), "Cannot move backward");
    }

    @Test
    void blackPawnBeforeRiverCanOnlyMoveForward() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 6),
            new ChessPiece(PieceType.PAWN, Side.BLACK, new Position(4, 6), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 6));
        assertEquals(1, moves.size(), "Black pawn before river should have 1 move");
        assertTrue(moves.contains(new Position(4, 5)), "Black pawn should move forward to row 5");
    }

    @Test
    void blackPawnAfterCrossingRiverCanMoveThreeWays() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 3),
            new ChessPiece(PieceType.PAWN, Side.BLACK, new Position(4, 3), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 3));
        assertTrue(moves.contains(new Position(4, 2)), "Forward");
        assertTrue(moves.contains(new Position(3, 3)), "Left");
        assertTrue(moves.contains(new Position(5, 3)), "Right");
        assertFalse(moves.contains(new Position(4, 4)), "Cannot move backward");
    }

    @Test
    void advisorMovesDiagonallyOneStep() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.ADVISOR, Side.RED, new Position(4, 5), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(3, 4)), "Diagonal top-left");
        assertTrue(moves.contains(new Position(5, 4)), "Diagonal top-right");
        assertTrue(moves.contains(new Position(3, 6)), "Diagonal bottom-left");
        assertTrue(moves.contains(new Position(5, 6)), "Diagonal bottom-right");
        assertFalse(moves.contains(new Position(4, 4)), "Should not move straight");
    }

    @Test
    void jeiqiAdvisorCanLeavePalace() {
        board = new ChessBoard();
        board.setPieceAt(new Position(3, 0),
            new ChessPiece(PieceType.ADVISOR, Side.RED, new Position(3, 0), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(3, 0));
        assertTrue(moves.contains(new Position(4, 1)), "Should move diagonally from palace");
    }

    @Test
    void elephantMovesTwoStepsDiagonally() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.ELEPHANT, Side.RED, new Position(4, 5), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.contains(new Position(2, 3)), "Top-left field");
        assertTrue(moves.contains(new Position(6, 3)), "Top-right field");
        assertTrue(moves.contains(new Position(2, 7)), "Bottom-left field");
        assertTrue(moves.contains(new Position(6, 7)), "Bottom-right field");
    }

    @Test
    void elephantShouldBeBlockedByEyePiece() {
        board = new ChessBoard();
        board.setPieceAt(new Position(4, 5),
            new ChessPiece(PieceType.ELEPHANT, Side.RED, new Position(4, 5), true));
        board.setPieceAt(new Position(3, 4),
            new ChessPiece(PieceType.PAWN, Side.BLACK, new Position(3, 4), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertFalse(moves.contains(new Position(2, 3)), "Blocked by eye at (3,4)");
        assertTrue(moves.contains(new Position(6, 3)), "Other directions still available");
    }

    @Test
    void jeiqiElephantCanCrossRiver() {
        board = new ChessBoard();
        board.setPieceAt(new Position(2, 4),
            new ChessPiece(PieceType.ELEPHANT, Side.RED, new Position(2, 4), true));
        List<Position> moves = gen.getLegalMoves(board, new Position(2, 4));
        assertTrue(moves.contains(new Position(4, 6)), "Should cross river to row 6");
    }

    @Test
    void emptyPositionReturnsEmptyMoves() {
        List<Position> moves = gen.getLegalMoves(board, new Position(4, 5));
        assertTrue(moves.isEmpty(), "Empty position should have no moves");
    }
}
