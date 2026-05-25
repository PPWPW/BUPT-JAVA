package com.jeiqi.protocol;

public enum MessageType {
    JOIN_QUEUE,
    LEAVE_QUEUE,
    MATCH_FOUND,
    GAME_START,
    MAKE_MOVE,
    MOVE_RESULT,
    BOARD_STATE,
    TURN_NOTIFY,
    TIMEOUT_WARN,
    GAME_OVER,
    RESIGN,
    DRAW_REQUEST,
    DRAW_RESPONSE,
    ERROR
}
