package com.jeiqi.service;

import com.jeiqi.model.GameNotation;
import com.jeiqi.repository.NotationRepository;

import java.util.List;
import java.util.Optional;

public class NotationService {

    private final NotationRepository notationRepository;
    private final com.jeiqi.repository.GameRepository gameRepository;

    public NotationService(NotationRepository notationRepository, com.jeiqi.repository.GameRepository gameRepository) {
        this.notationRepository = notationRepository;
        this.gameRepository = gameRepository;
    }

    public GameNotation saveNotation(GameNotation notation) {
        return notationRepository.save(notation);
    }

    public Optional<GameNotation> getNotation(String gameId) {
        return notationRepository.findByGameId(gameId);
    }

    public List<GameNotation> listNotations() {
        return notationRepository.findAllByOrderByGameDateDesc();
    }

    public void clearAllNotations() {
        // 优先清空对局表以释放外键约束关联
        gameRepository.deleteAll();
        // 随后清空棋谱表与历史走子记录
        notationRepository.deleteAll();
    }
}
