package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Item {
    private double x;
    private double y;
    private String type;
    private static Image woodImage;
    private static Image stoneImage;

    static {
        woodImage = new Image(Item.class.getResourceAsStream("/wood.png"));
        stoneImage = new Image(Item.class.getResourceAsStream("/stone.png"));
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

    public void render(GraphicsContext gc, double cameraX) {
        if (type.equals("wood")) {
            gc.drawImage(woodImage, x - cameraX, y, 30, 30);
        } else if (type.equals("stone")) {
            gc.drawImage(stoneImage, x - cameraX, y, 60, 60);
        }
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