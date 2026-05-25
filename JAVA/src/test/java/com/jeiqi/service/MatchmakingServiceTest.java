package com.jeiqi.service;

import com.jeiqi.model.Player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private GameService gameService;

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingService = new MatchmakingService(gameService);
    }

    @Test
    void shouldAddPlayerToQueue() {
        Player player = new Player("p1", "Alice");
        boolean result = matchmakingService.joinQueue(player);

        assertTrue(result);
        assertEquals(1, matchmakingService.getQueueSize());
    }

    @Test
    void shouldNotAddDuplicatePlayer() {
        Player player = new Player("p1", "Alice");
        matchmakingService.joinQueue(player);
        boolean result = matchmakingService.joinQueue(player);

        assertFalse(result);
        assertEquals(1, matchmakingService.getQueueSize());
    }

    @Test
    void shouldRemovePlayerFromQueue() {
        Player player = new Player("p1", "Alice");
        matchmakingService.joinQueue(player);
        boolean removed = matchmakingService.leaveQueue("p1");

        assertTrue(removed);
        assertEquals(0, matchmakingService.getQueueSize());
    }

    @Test
    void shouldNotMatchWithOnePlayer() {
        Player player = new Player("p1", "Alice");
        matchmakingService.joinQueue(player);

        Optional<String> gameId = matchmakingService.tryMatch();

        assertTrue(gameId.isEmpty());
        assertEquals(1, matchmakingService.getQueueSize());
    }

    @Test
    void shouldMatchTwoPlayers() {
        Player p1 = new Player("p1", "Alice");
        Player p2 = new Player("p2", "Bob");
        matchmakingService.joinQueue(p1);
        matchmakingService.joinQueue(p2);

        when(gameService.createGame(any(Player.class), any(Player.class)))
            .thenAnswer(inv -> {
                var g = new com.jeiqi.model.Game("game-test");
                return g;
            });

        Optional<String> gameId = matchmakingService.tryMatch();

        assertTrue(gameId.isPresent());
        assertEquals(0, matchmakingService.getQueueSize());
    }
}
