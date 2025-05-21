package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class NPC {
    private double x, y;
    private Image image;

    private static final double WIDTH = 40;
    private static final double HEIGHT = 60;

    public NPC(double x, double y, Image image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public void render(GraphicsContext gc, double cameraX) {
        gc.drawImage(image, x - cameraX, y, WIDTH, HEIGHT);
    }

    public boolean isNear(double px, double py, double pWidth, double pHeight) {
        return Math.abs(px - x) < 50 && Math.abs(py - y) < 50;
    }
}
