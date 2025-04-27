package cz.cvut.fel.pjv.alchemists_quest;

import javafx.geometry.Rectangle2D;

public class Player {
    private double x, y;
    private double width, height;
    private double velocityX, velocityY;
    private double speed = 200;
    private double jumpStrength = 500;
    private double gravity = 1000;
    private boolean onGround = false;

    // Конструктор тот же
    public Player(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    // Методы moveLeft, moveRight, stopHorizontalMovement, jump - те же
    public void moveLeft(double deltaTime) { velocityX = -speed; }
    public void moveRight(double deltaTime) { velocityX = speed; }
    public void stopHorizontalMovement() { velocityX = 0; }
    public void jump() { if (onGround) { velocityY = -jumpStrength; onGround = false; } }

    // Измененный метод update
    public void update(double deltaTime, Platform platform, double canvasWidth, double canvasHeight) {
        // Применяем гравитацию
        velocityY += gravity * deltaTime;

        // Обновляем позицию по X
        double nextX = x + velocityX * deltaTime;
        // Проверка границ по X
        if (nextX < 0) {
            nextX = 0;
            velocityX = 0; // Останавливаем у левой границы
        } else if (nextX + width > canvasWidth) {
            nextX = canvasWidth - width;
            velocityX = 0; // Останавливаем у правой границы
        }
        x = nextX;


        // Предварительное обновление Y для проверки столкновений
        double nextY = y + velocityY * deltaTime;

        // Проверка столкновения с платформой
        checkPlatformCollision(platform, nextY); // Передаем будущую позицию Y

        // Если после проверки столкновений мы все еще не на земле, обновляем Y
        if (!onGround) {
            y = nextY;
        }

        // Проверка падения за нижнюю границу (можно улучшить)
        if (y > canvasHeight) {
            System.out.println("Player has fallen!");
            // Возвращаем на стартовую позицию над платформой (пример)
            x = 100;
            y = canvasHeight - height - platform.getHeight() - 50; // Используем высоту платформы
            velocityY = 0;
            onGround = false; // Сброс состояния
        }
    }

    private void checkPlatformCollision(Platform platform, double nextY) {
        Rectangle2D nextPlayerBounds = new Rectangle2D(x, nextY, width, height); // Границы для СЛЕДУЮЩЕГО кадра по Y
        Rectangle2D platformBounds = platform.getBounds();
        Rectangle2D currentPlayerBounds = getBounds(); // Текущие границы

        onGround = false; // Сбрасываем флаг перед проверкой

        // Проверяем столкновение СВЕРХУ платформы
        // Условия:
        // 1. Игрок движется вниз (velocityY >= 0) - учитываем и случай стояния на месте
        // 2. Текущая нижняя граница игрока выше или на уровне верха платформы
        // 3. Следующая нижняя граница игрока ниже или на уровне верха платформы
        // 4. Есть пересечение по горизонтали
        if (velocityY >= 0 &&
                currentPlayerBounds.getMaxY() <= platformBounds.getMinY() &&
                nextPlayerBounds.getMaxY() >= platformBounds.getMinY() &&
                nextPlayerBounds.getMaxX() > platformBounds.getMinX() &&
                nextPlayerBounds.getMinX() < platformBounds.getMaxX())
        {
            this.y = platformBounds.getMinY() - height; // Ставим точно на платформу
            this.velocityY = 0; // Останавливаем падение
            onGround = true; // Игрок на земле
            //System.out.println("Collision Top!");
        }
        // Здесь нужна более сложная логика для столкновений с боков и снизу платформы,
        // если это необходимо для вашей игры. Пока только сверху.

        // Проверка падения с платформы (если мы были на ней)
        if (onGround && (currentPlayerBounds.getMaxX() <= platformBounds.getMinX() || currentPlayerBounds.getMinX() >= platformBounds.getMaxX())) {
            // onGround = false; // Фактически, onGround уже будет false после сброса в начале метода, если не было новой коллизии
        }
    }


    // Методы getBounds, геттеры - те же
    public Rectangle2D getBounds() { return new Rectangle2D(x, y, width, height); }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getVelocityY() { return velocityY; }
    public boolean isOnGround() { return onGround; }
}
