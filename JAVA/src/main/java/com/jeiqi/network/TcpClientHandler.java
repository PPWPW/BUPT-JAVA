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
import java.util.concurrent.ConcurrentHashMap;

public class TcpClientHandler implements Runnable {

    private static final ConcurrentHashMap<String, TcpClientHandler> activeConnections = new ConcurrentHashMap<>();

    private final Socket socket;
    private final ProtocolHandler protocolHandler;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private PrintWriter out;
    private String myPlayerId;

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
            if (myPlayerId != null) {
                activeConnections.remove(myPlayerId);
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case JOIN_QUEUE -> {
                String playerId = msg.getPlayerId();
                this.myPlayerId = playerId;
                activeConnections.put(playerId, this);

                Player player = new Player(playerId,
                    msg.getPayload() != null ? (String) msg.getPayload().get("username") : "Unknown");
                matchmakingService.joinQueue(player);

                Optional<String> gameIdOpt = matchmakingService.tryMatch();
                if (gameIdOpt.isPresent()) {
                    Game game = gameService.getGame(gameIdOpt.get());
                    if (game != null) {
                        // Notify Red
                        TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
                        if (red != null) {
                            red.send(new GameMessage(MessageType.MATCH_FOUND, game.getId(), game.getRedPlayerId(),
                                createMap("gameId", game.getId(), "assignedSide", "RED")));
                            red.send(new GameMessage(MessageType.GAME_START, game.getId(), game.getRedPlayerId(),
                                createMap("gameId", game.getId(), "turn", "RED", "pieces", getPiecesList(game))));
                        }
                        // Notify Black
                        TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());
                        if (black != null) {
                            black.send(new GameMessage(MessageType.MATCH_FOUND, game.getId(), game.getBlackPlayerId(),
                                createMap("gameId", game.getId(), "assignedSide", "BLACK")));
                            black.send(new GameMessage(MessageType.GAME_START, game.getId(), game.getBlackPlayerId(),
                                createMap("gameId", game.getId(), "turn", "RED", "pieces", getPiecesList(game))));
                        }
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
                Game game = gameService.getGame(msg.getGameId());

                if (!result.isValid()) {
                    send(new GameMessage(MessageType.ERROR, msg.getGameId(),
                        msg.getPlayerId(), createMap("message", result.getErrorMessage())));
                } else if (game != null) {
                    GameMessage moveResultMsg = new GameMessage(MessageType.MOVE_RESULT, msg.getGameId(), msg.getPlayerId(),
                        createMap(
                            "move", move.getSource() + move.getDestination(),
                            "captured", result.isCaptured(),
                            "revealedType", result.getRevealedType() != null ? result.getRevealedType().name() : null,
                            "pieces", getPiecesList(game)
                        ));

                    // Broadcast to both players
                    TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
                    if (red != null) red.send(moveResultMsg);
                    TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());
                    if (black != null) black.send(moveResultMsg);
                }
            }
            default -> send(new GameMessage(MessageType.ERROR, null, null,
                createMap("message", "Unknown message type: " + msg.getType())));
        }
    }

    private void send(GameMessage msg) {
        if (out != null) {
            out.println(protocolHandler.encode(msg));
        }
    }

    private java.util.List<Map<String, Object>> getPiecesList(Game game) {
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        com.jeiqi.model.ChessBoard board = game.getBoard();
        for (int r = 0; r < com.jeiqi.model.ChessBoard.ROWS; r++) {
            for (int c = 0; c < com.jeiqi.model.ChessBoard.COLS; c++) {
                com.jeiqi.model.ChessPiece piece = board.getPieceAt(c, r);
                if (piece != null && piece.isAlive()) {
                    list.add(createMap(
                        "type", piece.isRevealed() ? piece.getType().name() : null,
                        "side", piece.getSide().name(),
                        "revealed", piece.isRevealed(),
                        "position", Map.of("col", piece.getPosition().getCol(), "row", piece.getPosition().getRow()),
                        "alive", piece.isAlive()
                    ));
                }
            }
        }
        return list;
    }

    private Map<String, Object> createMap(Object... entries) {
        Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}
