package com.jeiqi.repository;

import com.jeiqi.model.GameNotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotationRepository extends JpaRepository<GameNotation, String> {
    Optional<GameNotation> findByGameId(String gameId);
}
