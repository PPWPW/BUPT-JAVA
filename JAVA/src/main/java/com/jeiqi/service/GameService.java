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
    private final com.jeiqi.engine.TimerManager timerManager;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public GameService(GameRepository gameRepository) {
        this(gameRepository, new com.jeiqi.engine.TimerManager(60000L, 5000L), new org.springframework.context.support.GenericApplicationContext());
    }

    public GameService(GameRepository gameRepository, com.jeiqi.engine.TimerManager timerManager, org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.activeGames = new ConcurrentHashMap<>();
        this.gameRepository = gameRepository;
        this.timerManager = timerManager;
        this.eventPublisher = eventPublisher;
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
        timerManager.startTimer(gameId, () -> triggerTimeout(gameId));
        return game;
    }

    public MoveResult processMove(String gameId, Move move) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return MoveResult.invalid("对局不存在或已结束");
        }
        MoveResult result = gameFlow.executeMove(game, move);
        if (game.getStatus() == GameStatus.FINISHED) {
            timerManager.cancelTimer(gameId);
        } else {
            timerManager.startTimer(gameId, () -> triggerTimeout(gameId));
        }
        return result;
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
        timerManager.cancelTimer(gameId);
        return result;
    }

    public static class DrawResult {
        public enum Status { REQUESTED, ACCEPTED, REJECTED }
        private final Status status;
        private final String requesterId;
        private final GameResult gameResult;
        public DrawResult(Status status, String requesterId, GameResult gameResult) {
            this.status = status;
            this.requesterId = requesterId;
            this.gameResult = gameResult;
        }
        public Status getStatus() { return status; }
        public String getRequesterId() { return requesterId; }
        public GameResult getGameResult() { return gameResult; }
    }

    public DrawResult handleDraw(String gameId, String playerId, boolean accept) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        if (game.getDrawRequestedBy() == null) {
            if (accept) {
                game.setDrawRequestedBy(playerId);
                return new DrawResult(DrawResult.Status.REQUESTED, playerId, null);
            }
        } else {
            if (playerId.equals(game.getDrawRequestedBy())) {
                return null;
            }
            if (accept) {
                game.setStatus(GameStatus.FINISHED);
                GameResult result = GameResult.draw(GameResult.EndReason.DRAW_AGREED);
                game.setResultReason("DRAW_AGREED");
                game.setGameEndTime(System.currentTimeMillis());

                if (game.getNotation() != null) {
                    game.getNotation().setResult("1/2-1/2");
                    game.getNotation().setReason("DRAW_AGREED");
                }

                gameRepository.save(game);
                activeGames.remove(gameId);
                timerManager.cancelTimer(gameId);
                return new DrawResult(DrawResult.Status.ACCEPTED, game.getDrawRequestedBy(), result);
            } else {
                String origRequester = game.getDrawRequestedBy();
                game.setDrawRequestedBy(null);
                return new DrawResult(DrawResult.Status.REJECTED, origRequester, null);
            }
        }
        return null;
    }

    public GameResult handleTimeout(String gameId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        game.setStatus(GameStatus.FINISHED);
        Side loser = game.getCurrentTurn();
        Side winner = (loser == Side.RED) ? Side.BLACK : Side.RED;
        GameResult result = GameResult.win(winner, GameResult.EndReason.TIMEOUT);

        game.setWinner(winner);
        game.setResultReason("TIMEOUT");
        game.setGameEndTime(System.currentTimeMillis());

        if (game.getNotation() != null) {
            game.getNotation().setResult(winner == Side.RED ? "1-0" : "0-1");
            game.getNotation().setReason("TIMEOUT");
        }

        gameRepository.save(game);
        activeGames.remove(gameId);
        return result;
    }

    private void triggerTimeout(String gameId) {
        GameResult result = handleTimeout(gameId);
        if (result != null) {
            eventPublisher.publishEvent(new com.jeiqi.event.GameTimeoutEvent(this, gameId, result));
        }
    }

    public Game getActiveGameForPlayer(String playerId) {
        for (Game game : activeGames.values()) {
            if ((game.getRedPlayerId() != null && game.getRedPlayerId().equals(playerId)) ||
                (game.getBlackPlayerId() != null && game.getBlackPlayerId().equals(playerId))) {
                return game;
            }
        }
        return null;
    }

    public GameResult handleDisconnect(String gameId, String playerId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        game.setStatus(GameStatus.FINISHED);
        Side winner = game.getRedPlayer().getId().equals(playerId) ? Side.BLACK : Side.RED;
        GameResult result = GameResult.win(winner, GameResult.EndReason.DISCONNECT);

        game.setWinner(winner);
        game.setResultReason("DISCONNECT");
        game.setGameEndTime(System.currentTimeMillis());

        if (game.getNotation() != null) {
            game.getNotation().setResult(winner == Side.RED ? "1-0" : "0-1");
            game.getNotation().setReason("DISCONNECT");
        }

        gameRepository.save(game);
        activeGames.remove(gameId);
        timerManager.cancelTimer(gameId);
        return result;
    }

    public Game createRoom(String roomId, Player creator, String sidePreference) {
        Game game = new Game(roomId);
        game.setStatus(GameStatus.WAITING);
        if ("black".equalsIgnoreCase(sidePreference)) {
            creator.setSide(Side.BLACK);
            game.setBlackPlayer(creator);
            game.setBlackPlayerId(creator.getId());
            game.setBlackPlayerName(creator.getName());
        } else {
            creator.setSide(Side.RED);
            game.setRedPlayer(creator);
            game.setRedPlayerId(creator.getId());
            game.setRedPlayerName(creator.getName());
        }
        activeGames.put(roomId, game);
        return game;
    }

    public Game joinRoom(String roomId, Player joiner) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            return null;
        }
        if (game.getStatus() != GameStatus.WAITING) {
            return null;
        }
        if (game.getRedPlayer() == null) {
            joiner.setSide(Side.RED);
            game.setRedPlayer(joiner);
            game.setRedPlayerId(joiner.getId());
            game.setRedPlayerName(joiner.getName());
        } else {
            joiner.setSide(Side.BLACK);
            game.setBlackPlayer(joiner);
            game.setBlackPlayerId(joiner.getId());
            game.setBlackPlayerName(joiner.getName());
        }
        game.start();
        timerManager.startTimer(roomId, () -> triggerTimeout(roomId));
        return game;
    }

    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public GameFlow getGameFlow() {
        return gameFlow;
    }
}
