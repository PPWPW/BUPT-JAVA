package com.jeiqi.engine;

import com.jeiqi.model.ChessBoard;
import com.jeiqi.model.ChessPiece;
import com.jeiqi.model.MoveRule;
import com.jeiqi.model.PieceType;
import com.jeiqi.model.Position;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HiddenPieceRule implements MoveRule {

    private final Map<PieceType, MoveRule> rules;

    public HiddenPieceRule() {
        this.rules = new EnumMap<>(PieceType.class);
        rules.put(PieceType.KING, new KingRule());
        rules.put(PieceType.CHARIOT, new ChariotRule());
        rules.put(PieceType.HORSE, new HorseRule());
        rules.put(PieceType.CANNON, new CannonRule());
        rules.put(PieceType.PAWN, new PawnRule());
        rules.put(PieceType.ADVISOR, new AdvisorRule());
        rules.put(PieceType.ELEPHANT, new ElephantRule());
    }

    @Override
    public List<Position> getLegalMoves(ChessBoard board, Position from) {
        PieceType initialType = getInitialPieceType(from);
        if (initialType == null) {
            return Collections.emptyList();
        }
        MoveRule rule = rules.get(initialType);
        if (rule == null) {
            return Collections.emptyList();
        }
        return rule.getLegalMoves(board, from);
    }

    /**
     * 根据坐标获取该格子在中国象棋中开局对应的棋子类型
     */
    public static PieceType getInitialPieceType(Position pos) {
        int col = pos.getCol();
        int row = pos.getRow();
        
        // 归一化行坐标，因为黑方在上方 (row >= 5)，我们将其镜像到红方的 0-4 行以简化逻辑
        int normalizedRow = row;
        if (row >= 5) {
            normalizedRow = 9 - row;
        }

        if (normalizedRow == 0) {
            if (col == 0 || col == 8) return PieceType.CHARIOT;
            if (col == 1 || col == 7) return PieceType.HORSE;
            if (col == 2 || col == 6) return PieceType.ELEPHANT;
            if (col == 3 || col == 5) return PieceType.ADVISOR;
            if (col == 4) return PieceType.KING;
        } else if (normalizedRow == 2) {
            if (col == 1 || col == 7) return PieceType.CANNON;
        } else if (normalizedRow == 3) {
            if (col == 0 || col == 2 || col == 4 || col == 6 || col == 8) return PieceType.PAWN;
        }

        return null;
    }
}

