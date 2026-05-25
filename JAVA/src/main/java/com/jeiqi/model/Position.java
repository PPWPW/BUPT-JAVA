package com.jeiqi.model;

import java.util.Objects;

public class Position {

    private static final String COL_LETTERS = "abcdefghi";

    private final int col;
    private final int row;

    public Position(int col, int row) {
        if (col < 0 || col > 8) {
            throw new IllegalArgumentException("col must be 0-8, got: " + col);
        }
        if (row < 0 || row > 9) {
            throw new IllegalArgumentException("row must be 0-9, got: " + row);
        }
        this.col = col;
        this.row = row;
    }

    public static Position fromAlgebraic(String s) {
        if (s == null || s.length() != 2) {
            throw new IllegalArgumentException("Invalid position: " + s);
        }
        char colChar = s.charAt(0);
        int col = COL_LETTERS.indexOf(colChar);
        if (col < 0) {
            throw new IllegalArgumentException("Invalid column: " + colChar);
        }
        int row = Character.digit(s.charAt(1), 10);
        if (row < 0 || row > 9) {
            throw new IllegalArgumentException("Invalid row: " + s.charAt(1));
        }
        return new Position(col, row);
    }

    public String toAlgebraic() {
        return "" + COL_LETTERS.charAt(col) + row;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position p)) return false;
        return col == p.col && row == p.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(col, row);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
