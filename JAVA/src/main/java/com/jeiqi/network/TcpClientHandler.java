package com.jeiqi.network;

import com.jeiqi.model.Game;
import com.jeiqi.model.GameResult;
import com.jeiqi.model.GameStatus;
import com.jeiqi.model.Move;
import com.jeiqi.model.MoveResult;
import com.jeiqi.model.Player;
import com.jeiqi.model.Side;
import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.PieceType;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;
import com.jeiqi.protocol.ProtocolHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TcpClientHandler implements Runnable {

    static final ConcurrentHashMap<String, TcpClientHandler> activeConnections = new ConcurrentHashMap<>();

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
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3) {
                    sendError(4001, "消息格式错误");
                    continue;
                }
                try {
                    int msgType = Integer.parseInt(parts[0]);
                    int declaredLen = Integer.parseInt(parts[1]);
                    String payload = parts[2];
                    if (payload.getBytes(StandardCharsets.UTF_8).length != declaredLen) {
                        sendError(4001, "帧长度校验失败");
                        continue;
                    }
                    handleTcpMessage(msgType, payload);
                } catch (Exception e) {
                    sendError(4001, "解析消息异常: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("TCP client disconnected: " + e.getMessage());
        } finally {
            if (myPlayerId != null) {
                activeConnections.remove(myPlayerId);
                matchmakingService.leaveQueue(myPlayerId);

                Game game = gameService.getActiveGameForPlayer(myPlayerId);
                if (game != null) {
                    GameResult result = gameService.handleDisconnect(game.getId(), myPlayerId);
                    if (result != null) {
                        String opponentId = myPlayerId.equals(game.getRedPlayerId()) ? game.getBlackPlayerId() : game.getRedPlayerId();
                        TcpClientHandler opp = activeConnections.get(opponentId);
                        if (opp != null) {
                            opp.send(6, (opponentId.equals(game.getRedPlayerId()) ? "0" : "1") + "|4|disconnect");
                        }
                    }
                }
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handleTcpMessage(int msgType, String payload) {
        switch (msgType) {
            case 1 -> handleLogin(payload);
            case 2 -> handleMove(payload);
            case 8 -> handleDraw(payload);
            case 9 -> handleResign();
            case 5 -> handleQuit();
            default -> sendError(4001, "未知消息类型");
        }
    }

    private void handleLogin(String payload) {
        String[] fields = payload.split("\\|", -1);
        if (fields.length < 2) {
            sendError(100, "参数不足");
            return;
        }

        int preferredColor = Integer.parseInt(fields[0]); // 0=red, 1=black
        String name = fields[1];
        String gameId = fields.length > 2 ? fields[2] : "";

        this.myPlayerId = name;
        activeConnections.put(myPlayerId, this);

        Player player = new Player(myPlayerId, name);
        player.setConnection("TCP");

        if (!gameId.isBlank()) {
            Game game = gameService.getGame(gameId);
            if (game == null) {
                sendError(200, "游戏房间不存在");
                return;
            }
            if (game.getStatus() != GameStatus.WAITING) {
                sendError(201, "游戏房间已满");
                return;
            }
            // Join custom room
            if (game.getRedPlayer() == null) {
                player.setSide(Side.RED);
                game.setRedPlayer(player);
                game.setRedPlayerId(player.getId());
                game.setRedPlayerName(player.getName());
            } else {
                player.setSide(Side.BLACK);
                game.setBlackPlayer(player);
                game.setBlackPlayerId(player.getId());
                game.setBlackPlayerName(player.getName());
            }
            // Send login ack
            int colorCode = player.getSide() == Side.RED ? 0 : 1;
            send(3, "LOGIN_ACK|" + game.getId() + "|" + colorCode + "|PLAYING");

            // Start game immediately as it is now full
            game.start();
            notifyGameStart(game);
        } else {
            // Matchmaking
            matchmakingService.joinQueue(player);
            send(3, "LOGIN_ACK||" + preferredColor + "|WAITING");

            Optional<String> matchedGameId = matchmakingService.tryMatch("TCP");
            if (matchedGameId.isPresent()) {
                Game game = gameService.getGame(matchedGameId.get());
                if (game != null) {
                    // Send LOGIN_ACK and GAME_START to both
                    TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
                    TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());

                    if (red != null) {
                        red.send(3, "LOGIN_ACK|" + game.getId() + "|0|PLAYING");
                        red.send(3, "GAME_START|0");
                        red.send(7, red.getBoardStatePayload(game));
                    }
                    if (black != null) {
                        black.send(3, "LOGIN_ACK|" + game.getId() + "|1|PLAYING");
                        black.send(3, "GAME_START|0");
                        black.send(7, black.getBoardStatePayload(game));
                    }
                }
            }
        }
    }

    private void notifyGameStart(Game game) {
        TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
        TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());

        if (red != null) {
            red.send(3, "GAME_START|0");
            red.send(7, red.getBoardStatePayload(game));
        }
        if (black != null) {
            black.send(3, "GAME_START|0");
            black.send(7, black.getBoardStatePayload(game));
        }
    }

    private void handleMove(String payload) {
        String[] fields = payload.split("\\|", -1);
        if (fields.length < 2) {
            sendError(100, "参数错误");
            return;
        }

        String src = fields[0];
        String dst = fields[1];

        Game game = gameService.getActiveGameForPlayer(myPlayerId);
        if (game == null) {
            sendError(108, "游戏未在进行中");
            return;
        }

        Move move = new Move();
        move.setSource(src);
        move.setDestination(dst);
        move.setSide(myPlayerId.equals(game.getRedPlayerId()) ? Side.RED : Side.BLACK);

        MoveResult result = gameService.processMove(game.getId(), move);

        if (!result.isValid()) {
            int errorCode = 101;
            String errStr = result.getErrorMessage();
            if (errStr != null) {
                if (errStr.contains("不是你的回合")) errorCode = 107;
                else if (errStr.contains("不能吃自己的棋子")) errorCode = 103;
                else if (errStr.contains("起始位置没有棋子")) errorCode = 110;
            }
            sendError(errorCode, errStr);
            return;
        }

        PieceType revealedType = result.getRevealedType();
        String flipCode = "";
        if (revealedType != null) {
            flipCode = String.valueOf(revealedType.getCode());
        }

        boolean isCaptureOfHidden = false;
        String capturedActualCode = "";
        if (result.isCaptured() && !game.getBoard().getCapturedPieces().isEmpty()) {
            ChessPiece lastCaptured = game.getBoard().getCapturedPieces().get(game.getBoard().getCapturedPieces().size() - 1);
            if (lastCaptured.isCapturedAsHidden()) {
                isCaptureOfHidden = true;
                capturedActualCode = String.valueOf(lastCaptured.getType().getCode());
            }
        }

        String flipResultForA = flipCode;
        String flipResultForB = flipCode;

        if (isCaptureOfHidden) {
            Side movingSide = myPlayerId.equals(game.getRedPlayerId()) ? Side.RED : Side.BLACK;
            if (movingSide == Side.RED) {
                if (flipCode.isEmpty()) {
                    flipResultForA = capturedActualCode;
                    flipResultForB = "NULL";
                }
            } else {
                if (flipCode.isEmpty()) {
                    flipResultForA = "NULL";
                    flipResultForB = capturedActualCode;
                }
            }
        }

        TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
        TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());

        long time = System.currentTimeMillis();

        if (red != null) {
            red.send(2, src + "|" + dst + "|" + flipResultForA + "|" + time + "|0");
            red.send(7, red.getBoardStatePayload(game));
        }

        if (black != null) {
            black.send(2, src + "|" + dst + "|" + flipResultForB + "|" + time + "|0");
            black.send(7, black.getBoardStatePayload(game));
        }

        if (result.isGameOver()) {
            GameResult gr = result.getGameResult();
            int winnerCode = gr.isDraw() ? -1 : (gr.getWinner() == Side.RED ? 0 : 1);
            int reasonCode = 0;
            switch (gr.getReason()) {
                case CHECKMATE -> reasonCode = 0;
                case STALEMATE -> reasonCode = 1;
                case TIMEOUT -> reasonCode = 2;
                case RESIGN -> reasonCode = 3;
                case DISCONNECT -> reasonCode = 4;
                case KING_CAPTURED -> reasonCode = 5;
                case NO_CAPTURE_DRAW -> reasonCode = 6;
                case PERPETUAL_CHECK -> reasonCode = 7;
                case DRAW_AGREED -> reasonCode = 9;
            }

            String gameOverPayload = winnerCode + "|" + reasonCode + "|" + gr.getReason().name().toLowerCase();
            if (red != null) red.send(6, gameOverPayload);
            if (black != null) black.send(6, gameOverPayload);
        } else {
            // Turn Change
            int turnColor = game.getCurrentTurn() == Side.RED ? 0 : 1;
            if (red != null) red.send(3, "TURN_CHANGE|" + turnColor);
            if (black != null) black.send(3, "TURN_CHANGE|" + turnColor);
        }
    }

    private void handleResign() {
        Game game = gameService.getActiveGameForPlayer(myPlayerId);
        if (game == null) return;

        GameResult result = gameService.resign(game.getId(), myPlayerId);
        if (result != null) {
            int winnerCode = result.getWinner() == Side.RED ? 0 : 1;
            String gameOverPayload = winnerCode + "|3|resign";

            TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
            TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());

            if (red != null) red.send(6, gameOverPayload);
            if (black != null) black.send(6, gameOverPayload);
        }
    }

    private void handleDraw(String payload) {
        Game game = gameService.getActiveGameForPlayer(myPlayerId);
        if (game == null) return;

        boolean accept = payload.equalsIgnoreCase("ACCEPT");
        boolean offer = payload.equalsIgnoreCase("OFFER");

        if (offer) {
            gameService.handleDraw(game.getId(), myPlayerId, true);
            String opponentId = myPlayerId.equals(game.getRedPlayerId()) ? game.getBlackPlayerId() : game.getRedPlayerId();
            TcpClientHandler opp = activeConnections.get(opponentId);
            if (opp != null) {
                opp.send(8, "OFFER");
            }
        } else {
            var drawResult = gameService.handleDraw(game.getId(), myPlayerId, accept);
            if (drawResult != null) {
                if (drawResult.getStatus() == GameService.DrawResult.Status.ACCEPTED) {
                    TcpClientHandler red = activeConnections.get(game.getRedPlayerId());
                    TcpClientHandler black = activeConnections.get(game.getBlackPlayerId());
                    if (red != null) red.send(6, "-1|9|draw_agreed");
                    if (black != null) black.send(6, "-1|9|draw_agreed");
                } else if (drawResult.getStatus() == GameService.DrawResult.Status.REJECTED) {
                    String requesterId = drawResult.getRequesterId();
                    TcpClientHandler req = activeConnections.get(requesterId);
                    if (req != null) {
                        req.send(8, "DECLINE");
                    }
                }
            }
        }
    }

    private void handleQuit() {
        if (myPlayerId != null) {
            activeConnections.remove(myPlayerId);
            matchmakingService.leaveQueue(myPlayerId);
        }
    }

    void send(int msgType, String payload) {
        if (out != null) {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            String frame = msgType + "|" + bytes.length + "|" + payload;
            out.println(frame);
        }
    }

    private void sendError(int errorCode, String desc) {
        send(4, errorCode + "|" + (desc != null ? desc : ""));
    }

    private String getBoardStatePayload(Game game) {
        StringBuilder sb = new StringBuilder();
        sb.append(game.getCurrentTurn() == Side.RED ? "0" : "1");
        ChessBoard board = game.getBoard();
        for (int r = 0; r < ChessBoard.ROWS; r++) {
            int rowIdx = 9 - r;
            sb.append("|");
            for (int c = 0; c < ChessBoard.COLS; c++) {
                ChessPiece piece = board.getPieceAt(c, rowIdx);
                if (piece == null || !piece.isAlive()) {
                    sb.append(".");
                } else if (!piece.isRevealed()) {
                    sb.append(piece.getSide() == Side.RED ? "0?" : "1?");
                } else {
                    int color = piece.getSide() == Side.RED ? 0 : 1;
                    int code = piece.getType().getCode();
                    sb.append(color).append(code);
                }
                if (c < ChessBoard.COLS - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
}
