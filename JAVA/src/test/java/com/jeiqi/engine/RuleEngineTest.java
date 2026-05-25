package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.PieceType;
import com.jeiqi.model.Player;
import com.jeiqi.model.Position;
import com.jeiqi.model.Side;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine engine;
    private Game game;

    @BeforeEach
    void setUp() {
        engine = new RuleEngine(new MoveGenerator());
        game = new Game("test");
        game.setRedPlayer(new Player("p1", "Red"));
        game.setBlackPlayer(new Player("p2", "Black"));
        game.start();
    }

    private Game createEmptyGame() {
        Game g = new Game("test-clean");
        g.setRedPlayer(new Player("p1", "Red"));
        g.setBlackPlayer(new Player("p2", "Black"));
        g.setStatus(com.jeiqi.model.GameStatus.PLAYING);
        return g;
    }

    @Test
    void validateMoveShouldRejectWrongTurn() {
        Move move = new Move("a9", "a8", null, Side.BLACK);
        move.setSide(Side.BLACK);
        MoveResult result = engine.validateMove(game, move);
        assertFalse(result.isValid(), "Should reject black moving when it's red's turn");
    }

    @Test
    void validateMoveShouldRejectEmptySource() {
        Move move = new Move("e5", "e6", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertFalse(result.isValid(), "Should reject move from empty square");
    }

    @Test
    void validateMoveShouldAllowHiddenPieceOneStepMove() {
        Move move = new Move("e3", "e4", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertTrue(result.isValid(), "Hidden piece one step forward should be valid");
    }

    @Test
    void validateMoveShouldRejectInvalidHiddenMove() {
        Move move = new Move("e3", "e7", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertFalse(result.isValid(), "Hidden piece 4 steps should be invalid");
    }

    @Test
    void validateMoveShouldAllowRevealInPlace() {
        Position pos = new Position(4, 3);
        ChessPiece piece = game.getBoard().getPieceAt(pos);
        assertNotNull(piece);
        assertFalse(piece.isRevealed());

        Move move = new Move("e3", "e3", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertTrue(result.isValid(),
            "Reveal in place should be valid for hidden piece, got: " + result.getErrorMessage());
    }

    @Test
    void validateMoveShouldRejectRevealInPlaceForRevealedPiece() {
        Move move = new Move("e0", "e0", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertFalse(result.isValid(), "Reveal in place should be rejected for revealed piece");
    }

    @Test
    void validateMoveShouldRejectCapturingOwnPiece() {
        Move move = new Move("e0", "e2", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = engine.validateMove(game, move);
        assertFalse(result.isValid(), "Cannot move onto own piece");
    }

    @Test
    void checkGameOverShouldDetectKingsFacingEachOther() {
        Game g = createEmptyGame();
        ChessBoard b = g.getBoard();

        ChessPiece redKing = new ChessPiece(PieceType.KING, Side.RED, new Position(4, 0), true);
        ChessPiece blackKing = new ChessPiece(PieceType.KING, Side.BLACK, new Position(4, 9), true);
        b.setPieceAt(new Position(4, 0), redKing);
        b.setPieceAt(new Position(4, 9), blackKing);
        b.addToSideList(redKing);
        b.addToSideList(blackKing);

        GameResult result = engine.checkGameOver(g);
        assertNotNull(result, "Should detect kings facing each other");
    }

    @Test
    void checkGameOverShouldNotTriggerWhenPieceBetweenKings() {
        Game g = createEmptyGame();
        ChessBoard b = g.getBoard();

        ChessPiece redKing = new ChessPiece(PieceType.KING, Side.RED, new Position(4, 0), true);
        ChessPiece blackKing = new ChessPiece(PieceType.KING, Side.BLACK, new Position(4, 9), true);
        ChessPiece blocker = new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 5), true);
        b.setPieceAt(new Position(4, 0), redKing);
        b.setPieceAt(new Position(4, 9), blackKing);
        b.setPieceAt(new Position(4, 5), blocker);
        b.addToSideList(redKing);
        b.addToSideList(blackKing);
        b.addToSideList(blocker);

        GameResult result = engine.checkGameOver(g);
        assertNull(result, "Should not detect kings facing when piece between them");
    }

    @Test
    void checkGameOverShouldDetectCheckmate() {
        Game g = createEmptyGame();
        ChessBoard b = g.getBoard();

        ChessPiece redKing = new ChessPiece(PieceType.KING, Side.RED, new Position(4, 0), true);
        ChessPiece blackKing = new ChessPiece(PieceType.KING, Side.BLACK, new Position(4, 9), true);
        b.setPieceAt(new Position(4, 0), redKing);
        b.setPieceAt(new Position(4, 9), blackKing);
        b.addToSideList(redKing);
        b.addToSideList(blackKing);

        ChessPiece rowChariot = new ChessPiece(PieceType.CHARIOT, Side.BLACK, new Position(0, 0), true);
        ChessPiece colChariot = new ChessPiece(PieceType.CHARIOT, Side.BLACK, new Position(4, 5), true);
        b.setPieceAt(new Position(0, 0), rowChariot);
        b.setPieceAt(new Position(4, 5), colChariot);
        b.addToSideList(rowChariot);
        b.addToSideList(colChariot);

        g.setCurrentTurn(Side.RED);
        GameResult result = engine.checkGameOver(g);
        assertNotNull(result, "Red king should be checkmated by two chariots, but got null");
        assertFalse(result.isDraw(), "Should not be a draw");
        assertEquals(Side.BLACK, result.getWinner());
    }

    @Test
    void isInCheckShouldDetectWhenKingUnderAttack() {
        Game g = createEmptyGame();
        ChessBoard b = g.getBoard();

        ChessPiece redKing = new ChessPiece(PieceType.KING, Side.RED, new Position(4, 0), true);
        ChessPiece chariot = new ChessPiece(PieceType.CHARIOT, Side.BLACK, new Position(4, 5), true);
        b.setPieceAt(new Position(4, 0), redKing);
        b.setPieceAt(new Position(4, 5), chariot);
        b.addToSideList(redKing);
        b.addToSideList(chariot);

        assertTrue(engine.isInCheck(b, Side.RED), "Red king should be in check from chariot");
    }

    @Test
    void isInCheckShouldReturnFalseWhenKingNotAttacked() {
        Game g = createEmptyGame();
        ChessBoard b = g.getBoard();

        ChessPiece redKing = new ChessPiece(PieceType.KING, Side.RED, new Position(4, 0), true);
        ChessPiece chariot = new ChessPiece(PieceType.CHARIOT, Side.BLACK, new Position(4, 5), true);
        ChessPiece pawn = new ChessPiece(PieceType.PAWN, Side.RED, new Position(4, 3), true);
        b.setPieceAt(new Position(4, 0), redKing);
        b.setPieceAt(new Position(4, 5), chariot);
        b.setPieceAt(new Position(4, 3), pawn);
        b.addToSideList(redKing);
        b.addToSideList(chariot);
        b.addToSideList(pawn);

        assertFalse(engine.isInCheck(b, Side.RED), "Pawn blocks the chariot, king not in check");
    }
}
