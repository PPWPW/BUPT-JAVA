package com.jeiqi.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void shouldCreatePositionWithValidCoordinates() {
        Position pos = new Position(0, 0);
        assertEquals(0, pos.getCol());
        assertEquals(0, pos.getRow());
    }

    @Test
    void shouldCreatePositionAtBoundaries() {
        Position pos = new Position(8, 9);
        assertEquals(8, pos.getCol());
        assertEquals(9, pos.getRow());
    }

    @Test
    void shouldThrowOnInvalidColumn() {
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(9, 0));
    }

    @Test
    void shouldThrowOnInvalidRow() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 10));
    }

    @ParameterizedTest
    @CsvSource({
        "a0, 0, 0",
        "i9, 8, 9",
        "e5, 4, 5",
        "a9, 0, 9",
        "i0, 8, 0",
        "b3, 1, 3",
        "h7, 7, 7"
    })
    void shouldConvertFromAlgebraic(String algebraic, int expectedCol, int expectedRow) {
        Position pos = Position.fromAlgebraic(algebraic);
        assertEquals(expectedCol, pos.getCol());
        assertEquals(expectedRow, pos.getRow());
    }

    @ParameterizedTest
    @CsvSource({
        "a0, 0, 0",
        "i9, 8, 9",
        "e5, 4, 5",
        "a9, 0, 9",
        "i0, 8, 0"
    })
    void shouldConvertToAlgebraic(String expected, int col, int row) {
        Position pos = new Position(col, row);
        assertEquals(expected, pos.toAlgebraic());
    }

    @Test
    void shouldRoundTripAlgebraic() {
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 10; row++) {
                Position pos = new Position(col, row);
                Position roundTripped = Position.fromAlgebraic(pos.toAlgebraic());
                assertEquals(pos, roundTripped);
            }
        }
    }

    @Test
    void shouldThrowOnNullAlgebraic() {
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic(null));
    }

    @Test
    void shouldThrowOnInvalidLengthAlgebraic() {
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic("a"));
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic("abc"));
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic(""));
    }

    @Test
    void shouldThrowOnInvalidColumnChar() {
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic("j0"));
        assertThrows(IllegalArgumentException.class, () -> Position.fromAlgebraic("A0"));
    }

    @Test
    void positionsWithSameCoordinatesShouldBeEqual() {
        Position p1 = new Position(3, 5);
        Position p2 = new Position(3, 5);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void positionsWithDifferentCoordinatesShouldNotBeEqual() {
        Position p1 = new Position(3, 5);
        Position p2 = new Position(4, 5);
        assertNotEquals(p1, p2);
    }

    @Test
    void toStringShouldReturnAlgebraic() {
        Position pos = new Position(0, 0);
        assertEquals("a0", pos.toString());

        Position pos2 = new Position(8, 9);
        assertEquals("i9", pos2.toString());
    }
}
