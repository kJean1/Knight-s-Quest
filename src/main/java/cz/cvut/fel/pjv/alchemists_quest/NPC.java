package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class NPC {
    private double x, y, width, height;

    public NPC(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GraphicsContext gc, double cameraX) {
        gc.setFill(Color.PURPLE);
        gc.fillRect(x - cameraX, y, width, height);
    }

    public boolean isNear(double playerX, double playerY, double playerWidth, double playerHeight) {
        double dx = (x + width / 2) - (playerX + playerWidth / 2);
        double dy = (y + height / 2) - (playerY + playerHeight / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < 100; // радиус взаимодействия
    }

    public double getX() { return x; }
    public double getY() { return y; }
}
