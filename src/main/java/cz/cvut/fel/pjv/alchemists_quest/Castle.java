package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.logging.*;

public class Castle {
    public static final Logger GAME_LOGGER = Logger.getLogger("KnightsQuestLogger");

    private double x, y;
    private double width = 80, height = 120;
    private Image image;

    public Castle(double x, double y) {
        this.x = x;
        this.y = y;
        try {
            image = new Image(getClass().getResourceAsStream("/castle.png"));
        } catch (Exception e) {
            GAME_LOGGER.severe("I couldn`t get an image of castle!: " + e.getMessage());
        }
    }

    public void render(GraphicsContext gc, double cameraX) {
        if (image != null) {
            gc.drawImage(image, x - cameraX, y - height + 20, width, height);
        }
    }

    public boolean intersects(Player player) {
        return player.getX() < x + width && player.getX() + player.getWidth() > x &&
                player.getY() < y + height && player.getY() + player.getHeight() > y;
    }
}

