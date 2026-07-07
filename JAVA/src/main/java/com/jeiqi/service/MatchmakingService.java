package com.jeiqi.service;

import com.jeiqi.model.Player;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchmakingService {

    private final ConcurrentLinkedQueue<Player> tcpQueue;
    private final ConcurrentLinkedQueue<Player> wsQueue;
    private final GameService gameService;

    public MatchmakingService(GameService gameService) {
        this.tcpQueue = new ConcurrentLinkedQueue<>();
        this.wsQueue = new ConcurrentLinkedQueue<>();
        this.gameService = gameService;
    }

    public boolean joinQueue(Player player) {
        String clientType = "TCP".equals(player.getConnection()) ? "TCP" : "WS";
        ConcurrentLinkedQueue<Player> queue = "TCP".equals(clientType) ? tcpQueue : wsQueue;
        boolean alreadyInQueue = queue.stream()
            .anyMatch(p -> p.getId().equals(player.getId()));
        if (alreadyInQueue) {
            return false;
        }
        queue.add(player);
        return true;
    }

    public boolean leaveQueue(String playerId) {
        boolean removedTcp = tcpQueue.removeIf(p -> p.getId().equals(playerId));
        boolean removedWs = wsQueue.removeIf(p -> p.getId().equals(playerId));
        return removedTcp || removedWs;
    }

    public Optional<String> tryMatch() {
        return tryMatch("WS");
    }

    public Optional<String> tryMatch(String clientType) {
        ConcurrentLinkedQueue<Player> queue = "TCP".equals(clientType) ? tcpQueue : wsQueue;
        if (queue.size() < 2) {
            return Optional.empty();
        }

        Player player1 = queue.poll();
        Player player2 = queue.poll();

        if (player2 == null) {
            if (player1 != null) {
                queue.add(player1);
            }
            return Optional.empty();
        }

        if (player1.getId().equals(player2.getId())) {
            queue.add(player1);
            return Optional.empty();
        }

        var game = gameService.createGame(player1, player2);
        return Optional.of(game.getId());
    }

    public int getQueueSize() {
        return tcpQueue.size() + wsQueue.size();
    }

    public int getQueueSize(String clientType) {
        return "TCP".equals(clientType) ? tcpQueue.size() : wsQueue.size();
    }
}
