package com.jeiqi.service;

import com.jeiqi.model.GameNotation;
import com.jeiqi.model.Move;
import com.jeiqi.model.Side;
import com.jeiqi.repository.NotationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotationServiceTest {

    @Mock
    private NotationRepository notationRepository;

    @Mock
    private com.jeiqi.repository.GameRepository gameRepository;

    private NotationService notationService;

    @BeforeEach
    void setUp() {
        notationService = new NotationService(notationRepository, gameRepository);
    }

    @Test
    void shouldGetNotationByGameId() {
        GameNotation notation = new GameNotation("g-001");
        notation.setRedPlayerName("Alice");
        notation.setBlackPlayerName("Bob");
        notation.addMove(new Move("e3", "e4", 1, Side.RED));
        notation.setResult("1-0");

        when(notationRepository.findByGameId("g-001")).thenReturn(Optional.of(notation));

        Optional<GameNotation> result = notationService.getNotation("g-001");

        assertTrue(result.isPresent());
        GameNotation n = result.get();
        assertEquals("g-001", n.getGameId());
        assertEquals("Alice", n.getRedPlayerName());
        assertEquals("Bob", n.getBlackPlayerName());
        assertEquals(1, n.getMoveCount());
        assertEquals("1-0", n.getResult());
    }

    @Test
    void shouldReturnEmptyForNonexistentNotation() {
        when(notationRepository.findByGameId("nonexistent"))
            .thenReturn(Optional.empty());

        Optional<GameNotation> result = notationService.getNotation("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSaveNotation() {
        GameNotation notation = new GameNotation("g-002");
        notation.setRedPlayerName("Red");
        notation.setBlackPlayerName("Black");

        when(notationRepository.save(notation)).thenReturn(notation);

        GameNotation saved = notationService.saveNotation(notation);

        assertNotNull(saved);
        assertEquals("g-002", saved.getGameId());
        verify(notationRepository).save(notation);
    }

    @Test
    void shouldListAllNotations() {
        GameNotation n1 = new GameNotation("g-001");
        GameNotation n2 = new GameNotation("g-002");

        when(notationRepository.findAll()).thenReturn(List.of(n1, n2));

        List<GameNotation> notations = notationService.listNotations();

        assertEquals(2, notations.size());
        verify(notationRepository).findAll();
    }
}
