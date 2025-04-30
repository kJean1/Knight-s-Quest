package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Bush {
    private double x, y;
    private double width = 60, height = 50;
    private boolean hasBerry = true;

    private static Image bushWithBerries;
    private static Image bushWithoutBerries;

    static {
        try {
            bushWithBerries = new Image(Bush.class.getResourceAsStream("/bush_wberries.png"));
            bushWithoutBerries = new Image(Bush.class.getResourceAsStream("/bush_noberries.png"));
        } catch (Exception e) {
            System.err.println("Error. I can`t get an image of bush! " + e.getMessage());
        }
    }

    public Bush(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void render(GraphicsContext gc, double cameraX) {
        Image img = hasBerry ? bushWithBerries : bushWithoutBerries;
        if (img != null) {
            gc.drawImage(img, x - cameraX, y, width, height);
        } else {
            gc.setFill(hasBerry ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.BROWN);
            gc.fillRect(x - cameraX, y, width, height);
        }
    }

    public boolean intersects(double px, double py, double pwidth, double pheight) {
        return px < x + width && px + pwidth > x &&
                py < y + height && py + pheight > y;
    }

    public boolean hasBerry() {
        return hasBerry;
    }

    public void pickBerry() {
        hasBerry = false;
    }

    public boolean isNear(double px, double py, double pw, double ph) {
        double dx = x + width / 2 - (px + pw / 2);
        double dy = y + height / 2 - (py + ph / 2);
        return Math.abs(dx) < 50 && Math.abs(dy) < 50;
    }

    public double getX() { return x; }
    public double getY() { return y; }
}
