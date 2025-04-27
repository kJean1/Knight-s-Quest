package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Item {
    private double x;
    private double y;
    private String type;
    private static Image woodImage;

    static {
        try {
            woodImage = new Image(Item.class.getResourceAsStream("/wood.png"));
        } catch (Exception e) {
            System.err.println("Не удалось загрузить wood.png: " + e.getMessage());
        }
    }

    public Item(double x, double y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public boolean intersects(double playerX, double playerY, double playerWidth, double playerHeight) {
        return (playerX < x + 30 &&
                playerX + playerWidth > x &&
                playerY < y + 30 &&
                playerY + playerHeight > y);
    }

    public void render(GraphicsContext gc) {
        if ("wood".equals(type)) {
            gc.drawImage(woodImage, x, y, 30, 30);
        }
        // Можно добавить другие типы предметов
    }

    public String getType() {
        return type;
    }
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}

