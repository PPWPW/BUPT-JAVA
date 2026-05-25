package com.jeiqi.repository;

import com.jeiqi.model.GameNotation;
import com.jeiqi.model.Move;
import com.jeiqi.model.Side;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class NotationRepositoryTest {

    @Autowired
    private NotationRepository notationRepository;

    @Test
    void shouldSaveAndFindByGameId() {
        GameNotation notation = new GameNotation("g-001");
        notation.setRedPlayerName("Alice");
        notation.setBlackPlayerName("Bob");
        notation.setResult("1-0");
        notation.setReason("CHECKMATE");

        Move move = new Move("b3", "b4", 1, Side.RED);
        notation.addMove(move);

        notationRepository.save(notation);

        Optional<GameNotation> found = notationRepository.findByGameId("g-001");
        assertTrue(found.isPresent());
        GameNotation n = found.get();
        assertEquals("Alice", n.getRedPlayerName());
        assertEquals("Bob", n.getBlackPlayerName());
        assertEquals("1-0", n.getResult());
        assertEquals("CHECKMATE", n.getReason());
        assertEquals(1, n.getMoveCount());
        assertEquals("b3", n.getMoves().get(0).getSource());
        assertEquals("b4", n.getMoves().get(0).getDestination());
    }

    @Test
    void shouldPersistNotationWithMultipleMoves() {
        GameNotation notation = new GameNotation("g-002");
        notation.setRedPlayerName("Red");
        notation.setBlackPlayerName("Black");

        notation.addMove(new Move("e3", "e4", null, Side.RED));
        notation.addMove(new Move("g6", "g5", 3, Side.BLACK));
        notation.addMove(new Move("a0", "a5", 1, Side.RED));

        notationRepository.save(notation);

        Optional<GameNotation> found = notationRepository.findByGameId("g-002");
        assertTrue(found.isPresent());
        assertEquals(3, found.get().getMoveCount());
    }
}
