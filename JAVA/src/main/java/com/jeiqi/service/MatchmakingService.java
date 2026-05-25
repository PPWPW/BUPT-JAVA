package com.jeiqi.service;

import com.jeiqi.model.Player;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchmakingService {

    private final ConcurrentLinkedQueue<Player> waitingQueue;
    private final GameService gameService;

    public MatchmakingService(GameService gameService) {
        this.waitingQueue = new ConcurrentLinkedQueue<>();
        this.gameService = gameService;
    }

    public boolean joinQueue(Player player) {
        boolean alreadyInQueue = waitingQueue.stream()
            .anyMatch(p -> p.getId().equals(player.getId()));
        if (alreadyInQueue) {
            return false;
        }
        waitingQueue.add(player);
        return true;
    }

    public boolean leaveQueue(String playerId) {
        return waitingQueue.removeIf(p -> p.getId().equals(playerId));
    }

    public Optional<String> tryMatch() {
        if (waitingQueue.size() < 2) {
            return Optional.empty();
        }

        Player player1 = waitingQueue.poll();
        Player player2 = waitingQueue.poll();

        if (player2 == null) {
            waitingQueue.add(player1);
            return Optional.empty();
        }

        if (player1.getId().equals(player2.getId())) {
            waitingQueue.add(player1);
            return Optional.empty();
        }

        var game = gameService.createGame(player1, player2);
        return Optional.of(game.getId());
    }

    public int getQueueSize() {
        return waitingQueue.size();
    }
}
