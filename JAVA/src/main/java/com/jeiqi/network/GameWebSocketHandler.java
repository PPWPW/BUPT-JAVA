package com.jeiqi.network;

import com.jeiqi.model.Game;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Player;
import com.jeiqi.model.Side;
import com.jeiqi.protocol.GameMessage;
import com.jeiqi.protocol.MessageType;
import com.jeiqi.protocol.ProtocolHandler;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class GameWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProtocolHandler protocolHandler;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate,
                                ProtocolHandler protocolHandler,
                                MatchmakingService matchmakingService,
                                GameService gameService) {
        this.messagingTemplate = messagingTemplate;
        this.protocolHandler = protocolHandler;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
    }

    @MessageMapping("/join")
    public void handleJoinQueue(@Payload String json) {
        GameMessage msg = protocolHandler.decode(json);
        Player player = new Player(msg.getPlayerId(),
            msg.getPayload() != null ? (String) msg.getPayload().get("username") : "Unknown");

        matchmakingService.joinQueue(player);

        Optional<String> gameIdOpt = matchmakingService.tryMatch();
        if (gameIdOpt.isPresent()) {
            Game game = gameService.getGame(gameIdOpt.get());
            if (game != null) {
                notifyPlayer(game.getRedPlayer().getId(), MessageType.MATCH_FOUND,
                    Map.of("gameId", game.getId(), "side", "RED"));
                notifyPlayer(game.getBlackPlayer().getId(), MessageType.MATCH_FOUND,
                    Map.of("gameId", game.getId(), "side", "BLACK"));

                notifyPlayer(game.getRedPlayer().getId(), MessageType.GAME_START,
                    Map.of("gameId", game.getId(), "turn", "RED"));
                notifyPlayer(game.getBlackPlayer().getId(), MessageType.GAME_START,
                    Map.of("gameId", game.getId(), "turn", "RED"));
            }
        }
    }

    @MessageMapping("/leave")
    public void handleLeaveQueue(@Payload String json) {
        GameMessage msg = protocolHandler.decode(json);
        matchmakingService.leaveQueue(msg.getPlayerId());
    }

    @MessageMapping("/move")
    public void handleMove(@Payload String json) {
        GameMessage msg = protocolHandler.decode(json);
        Map<String, Object> payload = msg.getPayload();

        Move move = new Move();
        move.setSource((String) payload.get("source"));
        move.setDestination((String) payload.get("destination"));
        move.setType(payload.get("type") != null ? (Integer) payload.get("type") : null);
        move.setSide(determineSide(msg.getPlayerId(), msg.getGameId()));

        MoveResult result = gameService.processMove(msg.getGameId(), move);
        Game game = gameService.getGame(msg.getGameId());

        if (!result.isValid()) {
            GameMessage errorMsg = new GameMessage(MessageType.ERROR, msg.getGameId(),
                msg.getPlayerId(), Map.of("message", result.getErrorMessage()));
            notifyPlayer(msg.getPlayerId(), errorMsg);
            return;
        }

        GameMessage moveResultMsg = new GameMessage(MessageType.MOVE_RESULT,
            msg.getGameId(), msg.getPlayerId(), Map.of(
                "move", move.getSource() + move.getDestination(),
                "captured", result.isCaptured(),
                "revealedType", result.getRevealedType() != null ? result.getRevealedType().name() : null
            ));
        notifyGame(msg.getGameId(), moveResultMsg);

        if (result.isGameOver()) {
            GameMessage gameOverMsg = new GameMessage(MessageType.GAME_OVER,
                msg.getGameId(), null, Map.of(
                    "winner", result.getGameResult().getWinner() != null
                        ? result.getGameResult().getWinner().name() : null,
                    "reason", result.getGameResult().getReason().name(),
                    "draw", result.getGameResult().isDraw()
                ));
            notifyGame(msg.getGameId(), gameOverMsg);
        } else if (game != null) {
            GameMessage turnMsg = new GameMessage(MessageType.TURN_NOTIFY,
                msg.getGameId(), null, Map.of("turn", game.getCurrentTurn().name()));
            notifyGame(msg.getGameId(), turnMsg);
        }
    }

    @MessageMapping("/resign")
    public void handleResign(@Payload String json) {
        GameMessage msg = protocolHandler.decode(json);
        var result = gameService.resign(msg.getGameId(), msg.getPlayerId());
        if (result != null) {
            GameMessage gameOverMsg = new GameMessage(MessageType.GAME_OVER,
                msg.getGameId(), null, Map.of(
                    "winner", result.getWinner().name(),
                    "reason", "RESIGN"
                ));
            notifyGame(msg.getGameId(), gameOverMsg);
        }
    }

    @MessageMapping("/draw")
    public void handleDrawRequest(@Payload String json) {
        GameMessage msg = protocolHandler.decode(json);
        boolean accept = msg.getPayload() != null
            && Boolean.TRUE.equals(msg.getPayload().get("accept"));
        var result = gameService.handleDraw(msg.getGameId(), msg.getPlayerId(), accept);
        if (result != null) {
            GameMessage gameOverMsg = new GameMessage(MessageType.GAME_OVER,
                msg.getGameId(), null, Map.of("draw", true, "reason", "DRAW_AGREED"));
            notifyGame(msg.getGameId(), gameOverMsg);
        }
    }

    private void notifyPlayer(String playerId, MessageType type, Map<String, Object> payload) {
        GameMessage msg = new GameMessage(type, null, null, payload);
        notifyPlayer(playerId, msg);
    }

    private void notifyPlayer(String playerId, GameMessage msg) {
        messagingTemplate.convertAndSendToUser(playerId, "/queue/game", protocolHandler.encode(msg));
    }

    private void notifyGame(String gameId, GameMessage msg) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, protocolHandler.encode(msg));
    }

    private Side determineSide(String playerId, String gameId) {
        Game game = gameService.getGame(gameId);
        if (game == null) return null;
        if (game.getRedPlayer() != null && game.getRedPlayer().getId().equals(playerId))
            return Side.RED;
        if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(playerId))
            return Side.BLACK;
        return null;
    }
}
