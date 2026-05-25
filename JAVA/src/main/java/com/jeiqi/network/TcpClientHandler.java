package com.jeiqi.network;

import com.jeiqi.model.Game;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Player;
import com.jeiqi.protocol.GameMessage;
import com.jeiqi.protocol.MessageType;
import com.jeiqi.protocol.ProtocolHandler;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;

public class TcpClientHandler implements Runnable {

    private final Socket socket;
    private final ProtocolHandler protocolHandler;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private PrintWriter out;

    public TcpClientHandler(Socket socket, ProtocolHandler protocolHandler,
                            MatchmakingService matchmakingService, GameService gameService) {
        this.socket = socket;
        this.protocolHandler = protocolHandler;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                GameMessage msg = protocolHandler.decode(line);
                handleMessage(msg);
            }
        } catch (Exception e) {
            System.err.println("TCP client disconnected: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case JOIN_QUEUE -> {
                Player player = new Player(msg.getPlayerId(),
                    (String) msg.getPayload().get("username"));
                matchmakingService.joinQueue(player);
                Optional<String> gameIdOpt = matchmakingService.tryMatch();
                if (gameIdOpt.isPresent()) {
                    Game game = gameService.getGame(gameIdOpt.get());
                    if (game != null) {
                        GameMessage matchMsg = new GameMessage(MessageType.MATCH_FOUND,
                            game.getId(), msg.getPlayerId(),
                            Map.of("gameId", game.getId()));
                        send(matchMsg);
                    }
                }
            }
            case MAKE_MOVE -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = msg.getPayload();
                Move move = new Move();
                move.setSource((String) payload.get("source"));
                move.setDestination((String) payload.get("destination"));
                move.setType(payload.get("type") != null ? (Integer) payload.get("type") : null);

                MoveResult result = gameService.processMove(msg.getGameId(), move);
                if (!result.isValid()) {
                    send(new GameMessage(MessageType.ERROR, msg.getGameId(),
                        msg.getPlayerId(), Map.of("message", result.getErrorMessage())));
                } else {
                    send(new GameMessage(MessageType.MOVE_RESULT, msg.getGameId(),
                        msg.getPlayerId(), Map.of(
                            "captured", result.isCaptured(),
                            "revealedType",
                            result.getRevealedType() != null ? result.getRevealedType().name() : null
                        )));
                }
            }
            default -> send(new GameMessage(MessageType.ERROR, null, null,
                Map.of("message", "Unknown message type: " + msg.getType())));
        }
    }

    private void send(GameMessage msg) {
        out.println(protocolHandler.encode(msg));
    }
}
