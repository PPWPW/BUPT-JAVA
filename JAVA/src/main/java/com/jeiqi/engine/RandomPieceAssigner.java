package com.jeiqi.engine;

import com.jeiqi.model.PieceType;
import com.jeiqi.model.Side;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomPieceAssigner {

    private static final int[] TYPE_LIMITS = {1, 2, 2, 2, 5, 2, 2};

    private final Random random;
    private final Map<Side, Map<PieceType, Integer>> remaining;

    public RandomPieceAssigner() {
        this.random = new Random();
        this.remaining = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            Map<PieceType, Integer> pool = new EnumMap<>(PieceType.class);
            PieceType[] types = PieceType.values();
            for (int i = 0; i < types.length; i++) {
                pool.put(types[i], TYPE_LIMITS[i]);
            }
            this.remaining.put(side, pool);
        }
    }

    public PieceType assignType(Side side) {
        Map<PieceType, Integer> pool = remaining.get(side);
        List<PieceType> available = new ArrayList<>();
        for (Map.Entry<PieceType, Integer> entry : pool.entrySet()) {
            if (entry.getKey() == PieceType.KING) continue;
            int count = entry.getValue();
            for (int i = 0; i < count; i++) {
                available.add(entry.getKey());
            }
        }
        if (available.isEmpty()) {
            throw new IllegalStateException("No more pieces available for " + side);
        }
        PieceType chosen = available.get(random.nextInt(available.size()));
        pool.put(chosen, pool.get(chosen) - 1);
        return chosen;
    }

    public void markUsed(Side side, PieceType type) {
        Map<PieceType, Integer> pool = remaining.get(side);
        pool.put(type, pool.get(type) - 1);
    }

    public int getRemaining(Side side, PieceType type) {
        return remaining.get(side).get(type);
    }
}
