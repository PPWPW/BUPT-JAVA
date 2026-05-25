package com.jeiqi.repository;

import com.jeiqi.model.Game;
import com.jeiqi.model.GameStatus;
import com.jeiqi.model.Side;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void shouldSaveAndFindGame() {
        Game game = new Game("game-001");
        game.setRedPlayerId("u1");
        game.setRedPlayerName("Alice");
        game.setBlackPlayerId("u2");
        game.setBlackPlayerName("Bob");
        game.setStatus(GameStatus.FINISHED);
        game.setWinner(Side.RED);
        game.setResultReason("CHECKMATE");
        gameRepository.save(game);

        Game found = gameRepository.findById("game-001").orElse(null);
        assertNotNull(found);
        assertEquals("Alice", found.getRedPlayerName());
        assertEquals("Bob", found.getBlackPlayerName());
        assertEquals(GameStatus.FINISHED, found.getStatus());
        assertEquals(Side.RED, found.getWinner());
    }

    @Test
    void shouldFindByPlayerId() {
        Game g1 = new Game("g-101");
        g1.setRedPlayerId("p1");
        g1.setBlackPlayerId("p2");
        g1.setStatus(GameStatus.FINISHED);
        gameRepository.save(g1);

        Game g2 = new Game("g-102");
        g2.setRedPlayerId("p3");
        g2.setBlackPlayerId("p1");
        g2.setStatus(GameStatus.FINISHED);
        gameRepository.save(g2);

        Game g3 = new Game("g-103");
        g3.setRedPlayerId("p2");
        g3.setBlackPlayerId("p3");
        g3.setStatus(GameStatus.FINISHED);
        gameRepository.save(g3);

        List<Game> p1Games = gameRepository.findByPlayerId("p1");
        assertEquals(2, p1Games.size());
    }

    @Test
    void shouldFindByStatus() {
        Game g1 = new Game("g-201");
        g1.setStatus(GameStatus.WAITING);
        gameRepository.save(g1);

        Game g2 = new Game("g-202");
        g2.setStatus(GameStatus.PLAYING);
        gameRepository.save(g2);

        Game g3 = new Game("g-203");
        g3.setStatus(GameStatus.PLAYING);
        gameRepository.save(g3);

        List<Game> playing = gameRepository.findByStatus(GameStatus.PLAYING);
        assertEquals(2, playing.size());

        List<Game> waiting = gameRepository.findByStatus(GameStatus.WAITING);
        assertEquals(1, waiting.size());
    }
}
