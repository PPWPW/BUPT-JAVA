package com.jeiqi.model;

public class Player {

    private String id;
    private String name;
    private Side side;
    private transient Object connection;

    public Player() {
    }

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Object getConnection() {
        return connection;
    }

    public void setConnection(Object connection) {
        this.connection = connection;
    }

    public boolean isRed() {
        return side == Side.RED;
    }

    public boolean isBlack() {
        return side == Side.BLACK;
    }

    @Override
    public String toString() {
        return "Player{id='" + id + "', name='" + name + "', side=" + side + "}";
    }
}
