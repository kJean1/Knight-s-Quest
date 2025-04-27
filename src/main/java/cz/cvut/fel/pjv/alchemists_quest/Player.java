package cz.cvut.fel.pjv.alchemists_quest;

import java.util.List;

public class Player {
    private double x, y;
    private double width, height;
    private double velocityX = 0;
    private double velocityY = 0;
    private double gravity = 800;
    private double moveSpeed = 200;
    private double jumpStrength = -600;
    private boolean onGround = false;

    public Player(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update(double deltaTime, List<Platform> platforms, double canvasWidth, double canvasHeight) {
        // Применяем гравитацию
        velocityY += gravity * deltaTime;

        // Обновляем позицию по X
        double nextX = x + velocityX * deltaTime;
        if (nextX < 0) {
            nextX = 0;
            velocityX = 0;
        } else if (nextX + width > canvasWidth) {
            nextX = canvasWidth - width;
            velocityX = 0;
        }
        x = nextX;

        // Предварительное обновление позиции по Y
        double nextY = y + velocityY * deltaTime;

        // По умолчанию считаем, что в воздухе
        onGround = false;

        // Проверяем столкновения со всеми платформами
        for (Platform platform : platforms) {
            if (checkPlatformCollision(platform, nextY)) {
                break; // Если встали на платформу, прекращаем проверку
            }
        }

        // Если не на земле, обновляем положение
        if (!onGround) {
            y = nextY;
        }

        // Проверка падения за границы экрана
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

    private boolean checkPlatformCollision(Platform platform, double nextY) {
        boolean horizontalOverlap = x + width > platform.getX() && x < platform.getX() + platform.getWidth();
        boolean fallingDown = velocityY >= 0; // Падаем вниз
        boolean willLand = (y + height <= platform.getY()) && (nextY + height >= platform.getY());

        if (horizontalOverlap && fallingDown && willLand) {
            y = platform.getY() - height;
            velocityY = 0;
            onGround = true;
            return true;
        }
        return false;
    }

    public void moveLeft(double deltaTime) {
        velocityX = -moveSpeed;
    }

    public void moveRight(double deltaTime) {
        velocityX = moveSpeed;
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
