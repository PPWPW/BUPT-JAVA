package com.jeiqi.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeiqi.model.*;
import com.jeiqi.repository.UserRepository;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;
import com.jeiqi.engine.HiddenPieceRule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> playerReadyMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private final UserRepository userRepository;

    public GameWebSocketHandler(ObjectMapper objectMapper,
                                 MatchmakingService matchmakingService,
                                 GameService gameService,
                                 UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = sessionUserMap.remove(session);
        if (userId != null) {
            activeSessions.remove(userId);
            playerReadyMap.remove(userId);
            matchmakingService.leaveQueue(userId);

            Game game = gameService.getActiveGameForPlayer(userId);
            if (game != null) {
                GameResult result = gameService.handleDisconnect(game.getId(), userId);
                if (result != null) {
                    String opponentId = userId.equals(game.getRedPlayerId()) ? game.getBlackPlayerId() : game.getRedPlayerId();
                    WebSocketSession oppSession = activeSessions.get(opponentId);
                    if (oppSession != null && oppSession.isOpen()) {
                        sendJson(oppSession, Map.of(
                            "messageType", "gameOver",
                            "winner", opponentId.equals(game.getRedPlayerId()) ? "red" : "black",
                            "reason", "disconnect",
                            "winnerId", opponentId
                        ));
                    }
                }
            }
        }
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String json = message.getPayload();
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            sendError(session, 4001, "JSON格式错误");
            return;
        }

        String messageType = (String) msg.get("messageType");
        if (messageType == null) {
            sendError(session, 4001, "未知 messageType");
            return;
        }

        switch (messageType) {
            case "Login" -> handleLogin(session, msg);
            case "register" -> handleRegister(session, msg);
            case "startMatch" -> handleStartMatch(session);
            case "cancelMatch" -> handleCancelMatch(session);
            case "Ready" -> handleReady(session);
            case "move" -> handleMove(session, msg);
            case "ping" -> handlePing(session, msg);
            case "Resign" -> handleResign(session);
            case "draw" -> handleDraw(session, msg);
            default -> sendError(session, 4001, "未知 messageType");
        }
    }

    private void handleLogin(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String username = (String) msg.get("userId");
        String password = (String) msg.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPasswordHash().equals(hashPassword(password))) {
            activeSessions.put(username, session);
            sessionUserMap.put(session, username);
            sendJson(session, Map.of(
                "messageType", "loginResult",
                "success", true,
                "message", "ok",
                "userId", username
            ));
        } else {
            sendJson(session, Map.of(
                "messageType", "loginResult",
                "success", false,
                "message", "用户名或密码错误",
                "userId", username != null ? username : ""
            ));
        }
    }

    private void handleRegister(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String username = (String) msg.get("userId");
        String password = (String) msg.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            sendJson(session, Map.of(
                "messageType", "loginResult",
                "success", false,
                "message", "用户名和密码不能为空",
                "userId", ""
            ));
            return;
        }

        if (userRepository.existsByUsername(username)) {
            sendJson(session, Map.of(
                "messageType", "loginResult",
                "success", false,
                "message", "用户名已存在",
                "userId", username
            ));
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        userRepository.save(user);

        activeSessions.put(username, session);
        sessionUserMap.put(session, username);
        sendJson(session, Map.of(
            "messageType", "loginResult",
            "success", true,
            "message", "ok",
            "userId", username
        ));
    }

    private void handleStartMatch(WebSocketSession session) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) {
            sendError(session, 1001, "请先登录");
            return;
        }

        Player player = new Player(userId, userId);
        player.setConnection("WS");
        matchmakingService.joinQueue(player);

        Optional<String> gameIdOpt = matchmakingService.tryMatch("WS");
        if (gameIdOpt.isPresent()) {
            Game game = gameService.getGame(gameIdOpt.get());
            if (game != null) {
                WebSocketSession redSession = activeSessions.get(game.getRedPlayerId());
                WebSocketSession blackSession = activeSessions.get(game.getBlackPlayerId());

                if (redSession != null && redSession.isOpen()) {
                    sendJson(redSession, Map.of(
                        "messageType", "matchSuccess",
                        "roomId", game.getId(),
                        "opponentId", game.getBlackPlayerId(),
                        "opponentNickname", game.getBlackPlayerName()
                    ));
                }
                if (blackSession != null && blackSession.isOpen()) {
                    sendJson(blackSession, Map.of(
                        "messageType", "matchSuccess",
                        "roomId", game.getId(),
                        "opponentId", game.getRedPlayerId(),
                        "opponentNickname", game.getRedPlayerName()
                    ));
                }
            }
        }
    }

    private void handleCancelMatch(WebSocketSession session) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId != null) {
            matchmakingService.leaveQueue(userId);
        }
    }

    private void handleReady(WebSocketSession session) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) return;

        playerReadyMap.put(userId, true);
        Game game = gameService.getActiveGameForPlayer(userId);
        if (game == null) return;

        String opponentId = userId.equals(game.getRedPlayerId()) ? game.getBlackPlayerId() : game.getRedPlayerId();
        WebSocketSession oppSession = activeSessions.get(opponentId);
        if (oppSession != null && oppSession.isOpen()) {
            sendJson(oppSession, Map.of(
                "messageType", "roomInfo",
                "opponentReady", true
            ));
        }

        if (Boolean.TRUE.equals(playerReadyMap.get(opponentId))) {
            game.start();

            WebSocketSession redSession = activeSessions.get(game.getRedPlayerId());
            WebSocketSession blackSession = activeSessions.get(game.getBlackPlayerId());

            List<Map<String, Object>> initialBoard = getInitialBoardList(game);

            if (redSession != null && redSession.isOpen()) {
                sendJson(redSession, Map.of(
                    "messageType", "gameStart",
                    "redPlayerId", game.getRedPlayerId(),
                    "blackPlayerId", game.getBlackPlayerId(),
                    "yourColor", "red",
                    "firstHand", true,
                    "initialBoard", initialBoard
                ));
            }

            if (blackSession != null && blackSession.isOpen()) {
                sendJson(blackSession, Map.of(
                    "messageType", "gameStart",
                    "redPlayerId", game.getRedPlayerId(),
                    "blackPlayerId", game.getBlackPlayerId(),
                    "yourColor", "black",
                    "firstHand", false,
                    "initialBoard", initialBoard
                ));
            }
        }
    }

    private void handleMove(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) return;

        Game game = gameService.getActiveGameForPlayer(userId);
        if (game == null) {
            sendError(session, 108, "游戏未在进行中");
            return;
        }

        String fromX = (String) msg.get("fromX");
        Integer fromY = (Integer) msg.get("fromY");
        String toX = (String) msg.get("toX");
        Integer toY = (Integer) msg.get("toY");
        Boolean isFlip = (Boolean) msg.get("isFlip");

        if (fromX == null || fromY == null || toX == null || toY == null || isFlip == null) {
            sendError(session, 101, "非法参数");
            return;
        }

        Move move = new Move();
        move.setSource(fromX + fromY);
        move.setDestination(toX + toY);
        move.setSide(userId.equals(game.getRedPlayerId()) ? Side.RED : Side.BLACK);

        MoveResult result = gameService.processMove(game.getId(), move);

        if (!result.isValid()) {
            int errorCode = 2001;
            String errStr = result.getErrorMessage();
            if (errStr != null) {
                if (errStr.contains("不是你的回合")) errorCode = 107;
                else if (errStr.contains("不合法的走法") || errStr.contains("不允许原地翻子")) errorCode = 101;
                else if (errStr.contains("不能吃自己的棋子")) errorCode = 103;
                else if (errStr.contains("起始位置没有棋子")) errorCode = 110;
            }
            sendError(session, errorCode, errStr);
            return;
        }

        PieceType revealedType = result.getRevealedType();
        String flipStr = null;
        if (revealedType != null) {
            String typeName = revealedType.name().toLowerCase();
            if (typeName.equals("chariot")) typeName = "rook";
            if (typeName.equals("horse")) typeName = "knight";
            if (typeName.equals("advisor")) typeName = "guard";
            if (typeName.equals("elephant")) typeName = "bishop";
            flipStr = typeName;
        }

        boolean isCaptureOfHidden = false;
        String capturedActualType = null;
        if (result.isCaptured() && !game.getBoard().getCapturedPieces().isEmpty()) {
            ChessPiece lastCaptured = game.getBoard().getCapturedPieces().get(game.getBoard().getCapturedPieces().size() - 1);
            if (lastCaptured.isCapturedAsHidden()) {
                isCaptureOfHidden = true;
                String typeName = lastCaptured.getType().name().toLowerCase();
                if (typeName.equals("chariot")) typeName = "rook";
                if (typeName.equals("horse")) typeName = "knight";
                if (typeName.equals("advisor")) typeName = "guard";
                if (typeName.equals("elephant")) typeName = "bishop";
                capturedActualType = typeName;
            }
        }

        String flipResultForA = flipStr;
        String flipResultForB = flipStr;
        String capturedTypeForA = null;
        String capturedTypeForB = null;

        Side movingSide = userId.equals(game.getRedPlayerId()) ? Side.RED : Side.BLACK;
        if (isCaptureOfHidden) {
            if (movingSide == Side.RED) {
                capturedTypeForA = capturedActualType;
                capturedTypeForB = "NULL";
                if (flipStr == null) {
                    flipResultForA = capturedActualType;
                    flipResultForB = "NULL";
                }
            } else {
                capturedTypeForA = "NULL";
                capturedTypeForB = capturedActualType;
                if (flipStr == null) {
                    flipResultForA = "NULL";
                    flipResultForB = capturedActualType;
                }
            }
        }

        WebSocketSession redSession = activeSessions.get(game.getRedPlayerId());
        WebSocketSession blackSession = activeSessions.get(game.getBlackPlayerId());

        Map<String, Object> moveMap = Map.of(
            "fromX", fromX,
            "fromY", fromY,
            "toX", toX,
            "toY", toY,
            "isFlip", isFlip
        );

        if (redSession != null && redSession.isOpen()) {
            Map<String, Object> res = new HashMap<>(Map.of(
                "messageType", "moveResult",
                "success", true,
                "valid", true,
                "move", moveMap
            ));
            if (flipResultForA != null) res.put("flipResult", flipResultForA);
            if (capturedTypeForA != null) res.put("capturedType", capturedTypeForA);
            sendJson(redSession, res);
        }

        if (blackSession != null && blackSession.isOpen()) {
            Map<String, Object> res = new HashMap<>(Map.of(
                "messageType", "moveResult",
                "success", true,
                "valid", true,
                "move", moveMap
            ));
            if (flipResultForB != null) res.put("flipResult", flipResultForB);
            if (capturedTypeForB != null) res.put("capturedType", capturedTypeForB);
            sendJson(blackSession, res);
        }

        if (result.isGameOver()) {
            GameResult gr = result.getGameResult();
            String reason = gr.getReason().name().toLowerCase();
            if (reason.equals("no_capture_draw")) reason = "draw_no_capture";
            else if (reason.equals("perpetual_check")) reason = "repetition_loss";

            String winnerSide = gr.isDraw() ? "draw" : (gr.getWinner() == Side.RED ? "red" : "black");
            String winnerId = gr.isDraw() ? null : (gr.getWinner() == Side.RED ? game.getRedPlayerId() : game.getBlackPlayerId());

            Map<String, Object> gameOverPayload = new HashMap<>(Map.of(
                "messageType", "gameOver",
                "winner", winnerSide,
                "reason", reason
            ));
            if (winnerId != null) {
                gameOverPayload.put("winnerId", winnerId);
            }

            if (redSession != null && redSession.isOpen()) sendJson(redSession, gameOverPayload);
            if (blackSession != null && blackSession.isOpen()) sendJson(blackSession, gameOverPayload);
        }
    }

    private void handleResign(WebSocketSession session) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) return;

        Game game = gameService.getActiveGameForPlayer(userId);
        if (game == null) return;

        GameResult result = gameService.resign(game.getId(), userId);
        if (result != null) {
            String winnerId = result.getWinner() == Side.RED ? game.getRedPlayerId() : game.getBlackPlayerId();
            Map<String, Object> gameOverMsg = Map.of(
                "messageType", "gameOver",
                "winner", result.getWinner() == Side.RED ? "red" : "black",
                "reason", "resign",
                "winnerId", winnerId
            );

            WebSocketSession redSession = activeSessions.get(game.getRedPlayerId());
            WebSocketSession blackSession = activeSessions.get(game.getBlackPlayerId());

            if (redSession != null && redSession.isOpen()) sendJson(redSession, gameOverMsg);
            if (blackSession != null && blackSession.isOpen()) sendJson(blackSession, gameOverMsg);
        }
    }

    private void handleDraw(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) return;

        Game game = gameService.getActiveGameForPlayer(userId);
        if (game == null) return;

        Boolean accept = (Boolean) msg.get("accept");
        if (accept == null) accept = true;

        var drawResult = gameService.handleDraw(game.getId(), userId, accept);
        if (drawResult != null) {
            String opponentId = userId.equals(game.getRedPlayerId()) ? game.getBlackPlayerId() : game.getRedPlayerId();
            WebSocketSession oppSession = activeSessions.get(opponentId);
            WebSocketSession reqSession = activeSessions.get(drawResult.getRequesterId());

            if (drawResult.getStatus() == com.jeiqi.service.GameService.DrawResult.Status.REQUESTED) {
                if (oppSession != null && oppSession.isOpen()) {
                    sendJson(oppSession, Map.of(
                        "messageType", "drawRequest"
                    ));
                }
            } else if (drawResult.getStatus() == com.jeiqi.service.GameService.DrawResult.Status.ACCEPTED) {
                Map<String, Object> gameOverPayload = Map.of(
                    "messageType", "gameOver",
                    "winner", "draw",
                    "reason", "draw_agreed"
                );
                WebSocketSession redSession = activeSessions.get(game.getRedPlayerId());
                WebSocketSession blackSession = activeSessions.get(game.getBlackPlayerId());
                if (redSession != null && redSession.isOpen()) sendJson(redSession, gameOverPayload);
                if (blackSession != null && blackSession.isOpen()) sendJson(blackSession, gameOverPayload);
            } else if (drawResult.getStatus() == com.jeiqi.service.GameService.DrawResult.Status.REJECTED) {
                if (reqSession != null && reqSession.isOpen()) {
                    sendJson(reqSession, Map.of(
                        "messageType", "drawRejected"
                    ));
                }
            }
        }
    }

    private void handlePing(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Object timestamp = msg.get("timestamp");
        sendJson(session, Map.of(
            "messageType", "pong",
            "timestamp", timestamp != null ? timestamp : System.currentTimeMillis()
        ));
    }

    private void sendJson(WebSocketSession session, Object obj) throws IOException {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(obj)));
                }
            }
        }
    }

    private void sendError(WebSocketSession session, int code, String message) throws IOException {
        sendJson(session, Map.of(
            "messageType", "error",
            "code", code,
            "message", message != null ? message : ""
        ));
    }

    private List<Map<String, Object>> getInitialBoardList(Game game) {
        List<Map<String, Object>> list = new ArrayList<>();
        ChessBoard board = game.getBoard();
        for (int r = 0; r < ChessBoard.ROWS; r++) {
            for (int c = 0; c < ChessBoard.COLS; c++) {
                ChessPiece piece = board.getPieceAt(c, r);
                if (piece != null) {
                    Position pos = piece.getPosition();
                    String x = "" + "abcdefghi".charAt(pos.getCol());
                    int y = pos.getRow();

                    String pieceType;
                    boolean visible;
                    if (piece.isKing()) {
                        pieceType = "king";
                        visible = true;
                    } else {
                        PieceType virtualType = HiddenPieceRule.getInitialPieceType(pos);
                        pieceType = virtualType != null ? virtualType.name().toLowerCase() : "";
                        if (pieceType.equals("chariot")) pieceType = "rook";
                        if (pieceType.equals("horse")) pieceType = "knight";
                        if (pieceType.equals("advisor")) pieceType = "guard";
                        if (pieceType.equals("elephant")) pieceType = "bishop";
                        visible = false;
                    }

                    list.add(Map.of(
                        "x", x,
                        "y", y,
                        "piece", pieceType,
                        "visible", visible
                    ));
                }
            }
        }
        return list;
    }

    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}
