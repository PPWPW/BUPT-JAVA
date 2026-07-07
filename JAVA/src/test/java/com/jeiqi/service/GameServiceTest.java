package com.jeiqi.service;

import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.GameStatus;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Player;
import com.jeiqi.model.Side;
import com.jeiqi.repository.GameRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository);
    }

    @Test
    void shouldCreateGameSuccessfully() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");

        Game game = gameService.createGame(red, black);

        assertNotNull(game);
        assertNotNull(game.getId());
        assertTrue(game.getId().startsWith("game-"));
        assertEquals(GameStatus.PLAYING, game.getStatus());
        assertEquals(Side.RED, red.getSide());
        assertEquals(Side.BLACK, black.getSide());
        assertEquals(red, game.getRedPlayer());
        assertEquals(black, game.getBlackPlayer());
    }

    @Test
    void processMoveShouldReturnInvalidForNonexistentGame() {
        Move move = new Move("e3", "e4", null, Side.RED);
        MoveResult result = gameService.processMove("nonexistent", move);

        assertFalse(result.isValid());
        assertEquals("对局不存在或已结束", result.getErrorMessage());
    }

    @Test
    void processMoveShouldReturnInvalidForWrongTurn() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        Move blackMove = new Move("g6", "g5", null, Side.BLACK);
        blackMove.setSide(Side.BLACK);
        MoveResult result = gameService.processMove(game.getId(), blackMove);

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void processMoveShouldAllowValidHiddenPieceMove() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        Move move = new Move("e3", "e4", null, Side.RED);
        move.setSide(Side.RED);
        MoveResult result = gameService.processMove(game.getId(), move);

        assertTrue(result.isValid(),
            "Hidden piece one step forward should be valid: " + result.getErrorMessage());
    }

    @Test
    void resignShouldEndGame() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        GameResult result = gameService.resign(game.getId(), "p1");

        assertNotNull(result);
        assertFalse(result.isDraw());
        assertEquals(Side.BLACK, result.getWinner());
        assertEquals(GameResult.EndReason.RESIGN, result.getReason());
        assertEquals(GameStatus.FINISHED, game.getStatus());
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void handleDrawShouldAcceptAndEndGame() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        // First player requests
        GameService.DrawResult reqResult = gameService.handleDraw(game.getId(), "p1", true);
        assertNotNull(reqResult);
        assertEquals(GameService.DrawResult.Status.REQUESTED, reqResult.getStatus());

        // Opponent accepts
        GameService.DrawResult acceptResult = gameService.handleDraw(game.getId(), "p2", true);
        assertNotNull(acceptResult);
        assertEquals(GameService.DrawResult.Status.ACCEPTED, acceptResult.getStatus());

        GameResult result = acceptResult.getGameResult();
        assertNotNull(result);
        assertTrue(result.isDraw());
        assertEquals(GameStatus.FINISHED, game.getStatus());
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void handleDrawShouldReturnNullWhenRejected() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        // First player requests
        gameService.handleDraw(game.getId(), "p1", true);

        // Opponent rejects
        GameService.DrawResult result = gameService.handleDraw(game.getId(), "p2", false);

        assertNotNull(result);
        assertEquals(GameService.DrawResult.Status.REJECTED, result.getStatus());
        assertEquals(GameStatus.PLAYING, game.getStatus());
    }

    @Test
    void processMoveOnFinishedGameShouldReturnInvalid() {
        Player red = new Player("p1", "Alice");
        Player black = new Player("p2", "Bob");
        Game game = gameService.createGame(red, black);

        gameService.resign(game.getId(), "p1");

        Move move = new Move("a0", "a1", null, Side.BLACK);
        move.setSide(Side.BLACK);
        MoveResult result = gameService.processMove(game.getId(), move);

        assertFalse(result.isValid());
    }
}
