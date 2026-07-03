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
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", game.getId());
        response.put("status", game.getStatus().name());
        response.put("redPlayer", game.getRedPlayerName());
        response.put("blackPlayer", game.getBlackPlayerName());
        response.put("currentTurn", game.getCurrentTurn() != null ? game.getCurrentTurn().name() : null);
        response.put("winner", game.getWinner() != null ? game.getWinner().name() : null);
        response.put("moves", game.getMoveHistory().size());
        return ResponseEntity.ok(response);
    }
}
