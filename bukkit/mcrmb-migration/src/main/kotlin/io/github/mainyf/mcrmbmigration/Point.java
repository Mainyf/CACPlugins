package io.github.mainyf.mcrmbmigration;

import java.util.UUID;

public class Point {

    private UUID source;

    private String id;

    private double value;

    private String password;

    public Point(UUID source, String id, double value, String password) {
        this.source = source;
        this.id = id;
        this.value = value;
        this.password = password;
    }

    public UUID getSource() {
        return source;
    }

    public void setSource(UUID source) {
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
