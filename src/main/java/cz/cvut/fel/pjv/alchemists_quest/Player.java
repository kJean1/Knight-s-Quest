package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private double x, y;
    private double width, height;
    private double velocityX = 0;
    private double velocityY = 0;
    private double gravity = 800;
    private double moveSpeed = 400;
    private double jumpStrength = -600;
    private boolean onGround = false;
    private boolean movingLeft = false;

    private List<Image> idleFrames;  // Кадры для бездействия (стояния)
    private List<Image> runFrames;   // Кадры для бега
    private int currentFrameIndex = 0;
    private boolean isIdle = true;   // Флаг для отслеживания, стоит ли игрок или двигается
    private long lastFrameTime = 0;
    private long frameDuration = 100_000_000; // 100 ms

    public Player(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        loadSpriteSheets();
    }

    public void update(double deltaTime, List<Platform> platforms, double canvasWidth, double canvasHeight, long now) {
        // Выбираем нужную анимацию в зависимости от движения
        if (velocityX != 0) {
            isIdle = false;  // Игрок двигается
        } else {
            isIdle = true;   // Игрок стоит
        }

        // Анимация
        List<Image> currentFrames = isIdle ? idleFrames : runFrames;  // Выбираем список кадров в зависимости от состояния
        if (!currentFrames.isEmpty() && now - lastFrameTime > frameDuration) {
            currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
            lastFrameTime = now;
        }

        // Остальная логика обновления (гравитация, движение и т.д.)
        velocityY += gravity * deltaTime;

        // Горизонтальное движение
        double nextX = x + velocityX * deltaTime;
        if (nextX < 0) {
            nextX = 0;
            velocityX = 0;
        } else if (nextX + width > canvasWidth) {
            nextX = canvasWidth - width;
            velocityX = 0;
        }
        x = nextX;

        // Вертикальное движение и столкновения
        double nextY = y + velocityY * deltaTime;
        onGround = false;

        for (Platform platform : platforms) {
            if (checkPlatformCollision(platform, nextY)) break;
        }

        if (!onGround) {
            y = nextY;
        }

        if (y > canvasHeight) {
            System.out.println("Player has fallen!");
            if (!platforms.isEmpty()) {
                Platform startPlatform = platforms.get(0);
                x = startPlatform.getX() + 50;
                y = startPlatform.getY() - height;
                velocityY = 0;
                onGround = false;
            }
        }
    }

    private void loadSpriteSheets() {
        try {
            // Загрузка спрайт-листов для стояния
            Image idleSpriteSheet = new Image(getClass().getResource("/player/Idle.png").toExternalForm());
            idleFrames = new ArrayList<>();
            int frameWidth = 192;  // Ширина одного кадра
            int frameHeight = 192; // Высота одного кадра
            int idleFrameCount = 5; // Количество кадров в спрайт-листе Idle (960 / 192 = 5)

            for (int i = 0; i < idleFrameCount; i++) {
                WritableImage frame = new WritableImage(idleSpriteSheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
                idleFrames.add(frame);
            }

            // Загрузка спрайт-листов для бега
            Image runSpriteSheet = new Image(getClass().getResource("/player/Run.png").toExternalForm());
            runFrames = new ArrayList<>();
            int runFrameCount = 6; // Количество кадров в спрайт-листе Run (1152 / 192 = 6)

            for (int i = 0; i < runFrameCount; i++) {
                WritableImage frame = new WritableImage(runSpriteSheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
                runFrames.add(frame);
            }
        } catch (Exception e) {
            System.err.println("Failed to load player sprite sheets.");
            e.printStackTrace();
            idleFrames = new ArrayList<>();
            runFrames = new ArrayList<>();
        }
    }

    public void restart(double x, double y) {
        if (onGround) {
            this.x = x;
            this.y = y;
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

    public void render(GraphicsContext gc, double cameraX) {
        List<Image> currentFrames = isIdle ? idleFrames : runFrames;

        if (!currentFrames.isEmpty()) {
            Image currentFrame = currentFrames.get(currentFrameIndex);

            double drawX = x - cameraX;
            double drawY = y - 54;
            double drawWidth = width * 2.5;
            double drawHeight = height * 2;

            if (!isIdle && movingLeft) {
                gc.save();
                gc.translate(drawX + drawWidth, 0);
                gc.scale(-1, 1);
                gc.drawImage(currentFrame, 0, drawY, drawWidth, drawHeight);
                gc.restore();
            }
            else {
                gc.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight);
            }
        }
    }


    public void moveLeft(double deltaTime) {
        velocityX = -moveSpeed;
        movingLeft = true;
    }
    public void moveRight(double deltaTime) {
        velocityX = moveSpeed;
        movingLeft = false;
    }

    public void stopHorizontalMovement() {
        velocityX = 0;
    }

    public void jump() {
        if (onGround) {
            velocityY = jumpStrength;
            onGround = false;
        }
    }

    public boolean isOnGround() {
        return onGround;
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
