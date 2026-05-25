package com.jeiqi.service;

import com.jeiqi.model.GameNotation;
import com.jeiqi.repository.NotationRepository;

import java.util.List;
import java.util.Optional;

public class NotationService {

    private final NotationRepository notationRepository;

    public NotationService(NotationRepository notationRepository) {
        this.notationRepository = notationRepository;
    }

    public GameNotation saveNotation(GameNotation notation) {
        return notationRepository.save(notation);
    }

    public Optional<GameNotation> getNotation(String gameId) {
        return notationRepository.findByGameId(gameId);
    }

    public List<GameNotation> listNotations() {
        return notationRepository.findAll();
    }
}
