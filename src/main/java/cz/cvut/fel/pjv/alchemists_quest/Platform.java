package cz.cvut.fel.pjv.alchemists_quest;

import javafx.geometry.Rectangle2D;

public class Platform {
    private double x, y;
    private double width, height;

    public Platform(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
