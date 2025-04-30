package cz.cvut.fel.pjv.alchemists_quest;

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

    public Player(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void restart(double x, double y){
        if (onGround == true)
        {
            this.x = x;
            this.y = y;
        }
    }

    public void update(double deltaTime, List<Platform> platforms, double canvasWidth, double canvasHeight) {
        velocityY += gravity * deltaTime;

        double nextX = x + velocityX * deltaTime;
        if (nextX < 0) {
            nextX = 0;
            velocityX = 0;
        } else if (nextX + width > canvasWidth) {
            nextX = canvasWidth - width;
            velocityX = 0;
        }
        x = nextX;

        double nextY = y + velocityY * deltaTime;

        onGround = false;

        for (Platform platform : platforms) {
            if (checkPlatformCollision(platform, nextY)) {
                break;
            }
        }

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
