package com.jeiqi.controller;

import com.jeiqi.model.Game;
import com.jeiqi.service.GameService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "id", game.getId(),
            "status", game.getStatus().name(),
            "redPlayer", game.getRedPlayerName(),
            "blackPlayer", game.getBlackPlayerName(),
            "currentTurn", game.getCurrentTurn().name(),
            "winner", game.getWinner() != null ? game.getWinner().name() : null,
            "moves", game.getMoveHistory().size()
        ));
    }
}
