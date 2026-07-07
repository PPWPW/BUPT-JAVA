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
    private static final ConcurrentHashMap<String, Set<WebSocketSession>> gameSpectators = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<WebSocketSession, String> sessionSpectatingMap = new ConcurrentHashMap<>();

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
        String spectatingRoomId = sessionSpectatingMap.remove(session);
        if (spectatingRoomId != null) {
            Set<WebSocketSession> specs = gameSpectators.get(spectatingRoomId);
            if (specs != null) {
                specs.remove(session);
                if (specs.isEmpty()) {
                    gameSpectators.remove(spectatingRoomId);
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
            case "createRoom" -> handleCreateRoom(session, msg);
            case "joinRoom" -> handleJoinRoom(session, msg);
            case "spectateGame" -> handleSpectateGame(session, msg);
            case "getBoardState" -> handleGetBoardState(session, msg);
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

        Set<WebSocketSession> specs = gameSpectators.get(game.getId());
        if (specs != null && !specs.isEmpty()) {
            Map<String, Object> res = new HashMap<>(Map.of(
                "messageType", "moveResult",
                "success", true,
                "valid", true,
                "move", moveMap
            ));
            if (flipStr != null) res.put("flipResult", flipStr);
            if (isCaptureOfHidden) {
                res.put("capturedType", "NULL");
                if (flipStr == null) {
                    res.put("flipResult", "NULL");
                }
            }
            for (WebSocketSession specSession : specs) {
                if (specSession.isOpen()) {
                    sendJson(specSession, res);
                }
            }
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

            if (specs != null && !specs.isEmpty()) {
                for (WebSocketSession specSession : specs) {
                    if (specSession.isOpen()) {
                        sendJson(specSession, gameOverPayload);
                    }
                }
            }
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

            Set<WebSocketSession> specs = gameSpectators.get(game.getId());
            if (specs != null && !specs.isEmpty()) {
                for (WebSocketSession specSession : specs) {
                    if (specSession.isOpen()) {
                        sendJson(specSession, gameOverMsg);
                    }
                }
            }
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

                Set<WebSocketSession> specs = gameSpectators.get(game.getId());
                if (specs != null && !specs.isEmpty()) {
                    for (WebSocketSession specSession : specs) {
                        if (specSession.isOpen()) {
                            sendJson(specSession, gameOverPayload);
                        }
                    }
                }
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

    private void handleCreateRoom(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) {
            sendError(session, 1001, "请先登录");
            return;
        }

        String roomId = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        while (gameService.getGame(roomId) != null) {
            roomId = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        }

        Player player = new Player(userId, userId);
        player.setConnection("WS");

        String sidePref = (String) msg.get("side");
        gameService.createRoom(roomId, player, sidePref);

        sendJson(session, Map.of(
            "messageType", "roomCreated",
            "roomId", roomId
        ));
    }

    private void handleJoinRoom(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String userId = sessionUserMap.get(session);
        if (userId == null) {
            sendError(session, 1001, "请先登录");
            return;
        }

        String roomId = (String) msg.get("roomId");
        if (roomId == null || roomId.isBlank()) {
            sendError(session, 2001, "请输入房间号");
            return;
        }

        Game game = gameService.getGame(roomId);
        if (game == null) {
            sendError(session, 2001, "房间不存在");
            return;
        }
        if (game.getStatus() != GameStatus.WAITING) {
            sendError(session, 2001, "房间已满或对局已开始");
            return;
        }

        Player player = new Player(userId, userId);
        player.setConnection("WS");

        Game joinedGame = gameService.joinRoom(roomId, player);
        if (joinedGame != null) {
            WebSocketSession redSession = activeSessions.get(joinedGame.getRedPlayerId());
            WebSocketSession blackSession = activeSessions.get(joinedGame.getBlackPlayerId());

            if (redSession != null && redSession.isOpen()) {
                sendJson(redSession, Map.of(
                    "messageType", "matchSuccess",
                    "roomId", joinedGame.getId(),
                    "opponentId", joinedGame.getBlackPlayerId(),
                    "opponentNickname", joinedGame.getBlackPlayerName()
                ));
            }
            if (blackSession != null && blackSession.isOpen()) {
                sendJson(blackSession, Map.of(
                    "messageType", "matchSuccess",
                    "roomId", joinedGame.getId(),
                    "opponentId", joinedGame.getRedPlayerId(),
                    "opponentNickname", joinedGame.getRedPlayerName()
                ));
            }
        } else {
            sendError(session, 2001, "加入房间失败");
        }
    }

    private void handleSpectateGame(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        if (roomId == null || roomId.isBlank()) {
            sendError(session, 2002, "请输入房间号");
            return;
        }

        Game game = gameService.getGame(roomId);
        if (game == null) {
            sendError(session, 2002, "房间不存在");
            return;
        }

        gameSpectators.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionSpectatingMap.put(session, roomId);

        sendBoardState(session, game);
    }

    private void handleGetBoardState(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        if (roomId == null || roomId.isBlank()) {
            sendError(session, 2002, "请输入房间号");
            return;
        }

        Game game = gameService.getGame(roomId);
        if (game == null) {
            sendError(session, 2002, "房间不存在");
            return;
        }

        sendBoardState(session, game);
    }

    private void sendBoardState(WebSocketSession session, Game game) throws IOException {
        String userId = sessionUserMap.get(session);
        String mySide = null;
        if (userId != null) {
            if (userId.equals(game.getRedPlayerId())) {
                mySide = "red";
            } else if (userId.equals(game.getBlackPlayerId())) {
                mySide = "black";
            }
        }

        List<Map<String, Object>> piecesList = new ArrayList<>();
        ChessBoard board = game.getBoard();
        for (int r = 0; r < ChessBoard.ROWS; r++) {
            for (int c = 0; c < ChessBoard.COLS; c++) {
                ChessPiece piece = board.getPieceAt(c, r);
                if (piece != null) {
                    Map<String, Object> pMap = new HashMap<>();
                    Position pos = piece.getPosition();
                    pMap.put("position", Map.of("col", pos.getCol(), "row", pos.getRow()));
                    pMap.put("side", piece.getSide().name().toLowerCase());
                    pMap.put("revealed", piece.isRevealed());
                    pMap.put("alive", piece.isAlive());

                    if (piece.isRevealed()) {
                        String typeName = piece.getType().name().toLowerCase();
                        if (typeName.equals("chariot")) typeName = "rook";
                        if (typeName.equals("horse")) typeName = "knight";
                        if (typeName.equals("advisor")) typeName = "guard";
                        if (typeName.equals("elephant")) typeName = "bishop";
                        pMap.put("type", typeName);
                    } else {
                        pMap.put("type", null);
                    }
                    piecesList.add(pMap);
                }
            }
        }

        List<Map<String, Object>> capturedList = new ArrayList<>();
        for (ChessPiece piece : board.getCapturedPieces()) {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("side", piece.getSide().name().toLowerCase());
            pMap.put("position", Map.of("col", piece.getPosition().getCol(), "row", piece.getPosition().getRow()));

            boolean revealedToMe = false;
            if (!piece.isCapturedAsHidden()) {
                revealedToMe = true;
            } else if (mySide != null) {
                if (piece.getSide() == Side.BLACK && mySide.equals("red")) {
                    revealedToMe = true;
                } else if (piece.getSide() == Side.RED && mySide.equals("black")) {
                    revealedToMe = true;
                }
            }

            if (revealedToMe) {
                pMap.put("revealed", true);
                String typeName = piece.getType().name().toLowerCase();
                if (typeName.equals("chariot")) typeName = "rook";
                if (typeName.equals("horse")) typeName = "knight";
                if (typeName.equals("advisor")) typeName = "guard";
                if (typeName.equals("elephant")) typeName = "bishop";
                pMap.put("type", typeName);
            } else {
                pMap.put("revealed", false);
                pMap.put("type", null);
            }
            capturedList.add(pMap);
        }

        List<Map<String, Object>> moveHistoryList = new ArrayList<>();
        int moveNum = 1;
        for (Move move : game.getMoveHistory()) {
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("source", move.getSource());
            mMap.put("destination", move.getDestination());
            mMap.put("type", move.getType());
            mMap.put("moveNumber", moveNum++);
            mMap.put("side", move.getSide().name().toLowerCase());
            mMap.put("revealMove", move.getSource().equals(move.getDestination()));
            moveHistoryList.add(mMap);
        }

        sendJson(session, Map.of(
            "messageType", "boardState",
            "roomId", game.getId(),
            "status", game.getStatus().name(),
            "redPlayerName", game.getRedPlayerName() != null ? game.getRedPlayerName() : "",
            "blackPlayerName", game.getBlackPlayerName() != null ? game.getBlackPlayerName() : "",
            "currentTurn", game.getCurrentTurn() != null ? game.getCurrentTurn().name().toLowerCase() : "red",
            "mySide", mySide != null ? mySide : "spectator",
            "pieces", piecesList,
            "capturedPieces", capturedList,
            "moveHistory", moveHistoryList
        ));
    }
}
