package com.jeiqi.service;

import com.jeiqi.engine.GameFlow;
import com.jeiqi.engine.RandomPieceAssigner;
import com.jeiqi.engine.RuleEngine;
import com.jeiqi.engine.MoveGenerator;
import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.GameStatus;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Player;
import com.jeiqi.model.Side;
import com.jeiqi.repository.GameRepository;

import java.util.concurrent.ConcurrentHashMap;

public class GameService {

    private final ConcurrentHashMap<String, Game> activeGames;
    private final GameFlow gameFlow;
    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.activeGames = new ConcurrentHashMap<>();
        this.gameRepository = gameRepository;
        MoveGenerator moveGenerator = new MoveGenerator();
        RuleEngine ruleEngine = new RuleEngine(moveGenerator);
        RandomPieceAssigner pieceAssigner = new RandomPieceAssigner();
        this.gameFlow = new GameFlow(ruleEngine, pieceAssigner);
    }

    public Game createGame(Player redPlayer, Player blackPlayer) {
        String gameId = "game-" + System.currentTimeMillis();
        Game game = new Game(gameId);
        redPlayer.setSide(Side.RED);
        blackPlayer.setSide(Side.BLACK);
        game.setRedPlayer(redPlayer);
        game.setBlackPlayer(blackPlayer);
        game.setRedPlayerId(redPlayer.getId());
        game.setRedPlayerName(redPlayer.getName());
        game.setBlackPlayerId(blackPlayer.getId());
        game.setBlackPlayerName(blackPlayer.getName());
        game.start();
        activeGames.put(gameId, game);
        return game;
    }

    public MoveResult processMove(String gameId, Move move) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return MoveResult.invalid("对局不存在或已结束");
        }
        return gameFlow.executeMove(game, move);
    }

    public GameResult resign(String gameId, String playerId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        game.setStatus(GameStatus.FINISHED);
        Side winner = game.getRedPlayer().getId().equals(playerId) ? Side.BLACK : Side.RED;
        GameResult result = GameResult.win(winner, GameResult.EndReason.RESIGN);

        game.setWinner(winner);
        game.setResultReason("RESIGN");
        game.setGameEndTime(System.currentTimeMillis());

        if (game.getNotation() != null) {
            game.getNotation().setResult(winner == Side.RED ? "1-0" : "0-1");
            game.getNotation().setReason("RESIGN");
        }

        gameRepository.save(game);
        activeGames.remove(gameId);
        return result;
    }

    public GameResult handleDraw(String gameId, String playerId, boolean accept) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        if (accept) {
            game.setStatus(GameStatus.FINISHED);
            GameResult result = GameResult.draw(GameResult.EndReason.RESIGN);
            game.setResultReason("DRAW_AGREED");
            game.setGameEndTime(System.currentTimeMillis());

            if (game.getNotation() != null) {
                game.getNotation().setResult("1/2-1/2");
                game.getNotation().setReason("DRAW_AGREED");
            }

            gameRepository.save(game);
            activeGames.remove(gameId);
            return result;
        }
        return null;
    }

    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public GameFlow getGameFlow() {
        return gameFlow;
    }
}
