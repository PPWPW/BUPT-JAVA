package com.jeiqi.engine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerManager {

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> gameTimers;
    private final long timeoutMs;
    private final long toleranceMs;

    public TimerManager(long timeoutMs, long toleranceMs) {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "timer");
            t.setDaemon(true);
            return t;
        });
        this.gameTimers = new ConcurrentHashMap<>();
        this.timeoutMs = timeoutMs;
        this.toleranceMs = toleranceMs;
    }

    public void startTimer(String gameId, Runnable onTimeout) {
        cancelTimer(gameId);
        ScheduledFuture<?> future = scheduler.schedule(
            () -> {
                gameTimers.remove(gameId);
                onTimeout.run();
            },
            timeoutMs + toleranceMs,
            TimeUnit.MILLISECONDS
        );
        gameTimers.put(gameId, future);
    }

    public void resetTimer(String gameId) {
        ScheduledFuture<?> future = gameTimers.get(gameId);
        if (future != null) {
            future.cancel(false);
            gameTimers.remove(gameId);
        }
    }

    public void cancelTimer(String gameId) {
        resetTimer(gameId);
    }

    public void shutdown() {
        gameTimers.values().forEach(f -> f.cancel(false));
        gameTimers.clear();
        scheduler.shutdown();
    }

    public int getRemainingSeconds(String gameId) {
        ScheduledFuture<?> future = gameTimers.get(gameId);
        if (future != null) {
            long delayMs = future.getDelay(TimeUnit.MILLISECONDS);
            long remainingMs = Math.max(0, delayMs - toleranceMs);
            return (int) Math.min(60, (remainingMs + 999) / 1000);
        }
        return 60;
    }
}
