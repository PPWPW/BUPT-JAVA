package com.jeiqi.repository;

import com.jeiqi.model.Game;
import com.jeiqi.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, String> {

    @Query("SELECT g FROM Game g WHERE g.redPlayerId = :playerId OR g.blackPlayerId = :playerId")
    List<Game> findByPlayerId(String playerId);

    List<Game> findByStatus(GameStatus status);
}
