package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.List;

public class Enemy {
    private double x, y;
    private final double width, height;
    private double velocityX = 100;
    private double velocityY = 0;
    private double gravity = 800;
    private boolean onGround = false;
    private boolean movingRight = true;

    private List<Image> runFrames;
    private int currentFrameIndex = 0;
    private long lastFrameTime = 0;
    private long frameDuration = 120_000_000;

    public Enemy(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        loadSpriteSheets();
    }

    public void update(double deltaTime, List<Platform> platforms, long now) {
        velocityY += gravity * deltaTime;
        double nextY = y + velocityY * deltaTime;

        onGround = false;
        Platform stoodPlatform = null;
        for (Platform platform : platforms) {
            if (checkPlatformCollision(platform, nextY)) {
                stoodPlatform = platform;
                break;
            }
        }

        if (onGround && stoodPlatform != null) {
            double footX = movingRight ? (x + width + velocityX * deltaTime) : (x - velocityX * deltaTime);
            boolean hasGround = false;
            for (Platform p : platforms) {
                if (footX + 5 > p.getX() && footX < p.getX() + p.getWidth()) {
                    if(y + height + 1 >= p.getY() && y + height + 1 <= p.getY() + 5) {
                        hasGround = true;
                        break;
                    }
                }
            }
            if (!hasGround) {
                movingRight = !movingRight;
            }
        }

        x += (movingRight ? velocityX : -velocityX) * deltaTime;

        if (!onGround) {
            y = nextY;
        }

        if (!runFrames.isEmpty() && now - lastFrameTime > frameDuration) {
            currentFrameIndex = (currentFrameIndex + 1) % runFrames.size();
            lastFrameTime = now;
        }
    }

    private boolean checkPlatformCollision(Platform platform, double nextY) {
        boolean horizontalOverlap = x + width > platform.getX() && x < platform.getX() + platform.getWidth();
        boolean fallingDown = velocityY >= 0;
        boolean willLand = (y + height <= platform.getY()) && (nextY + height >= platform.getY());

        if (horizontalOverlap && fallingDown && willLand) {
            y = platform.getY() - height;
            velocityY = 0;
            onGround = true;
            return true;
        }
        return false;
    }

    private void loadSpriteSheets() {
            Image walkSpriteSheet = new Image(getClass().getResource("/player/Run.png").toExternalForm());
            runFrames = new ArrayList<>();
            int frameWidth = 192;
            int frameHeight = 192;
            int walkFrameCount = 6;
            for (int i = 0; i < walkFrameCount; i++) {
                WritableImage frame = new WritableImage(walkSpriteSheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
                runFrames.add(frame);
            }
    }

    public void render(GraphicsContext gc, double cameraX) {
        if (!runFrames.isEmpty()) {
            Image currentFrame = runFrames.get(currentFrameIndex);

            double drawX = x - cameraX;
            double drawY = y - 54;
            double drawWidth = width * 2.5;
            double drawHeight = height * 2;

            if (!movingRight) {
                gc.save();
                gc.translate(drawX + drawWidth, 0);
                gc.scale(-1, 1);
                gc.drawImage(currentFrame, 0, drawY, drawWidth, drawHeight);
                gc.restore();
            } else {
                gc.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight);
            }
        }
        else
        {
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(x - cameraX, y - 54, width * 2.5, height * 2);
        }
    }

    public boolean collidesWith(Player player) {
        return x < player.getX() + player.getWidth() &&
                x + width > player.getX() &&
                y < player.getY() + player.getHeight() &&
                y + height > player.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    public double getWidth() {
        return width;
    }
    public double getHeight() {
        return height;
    }
}