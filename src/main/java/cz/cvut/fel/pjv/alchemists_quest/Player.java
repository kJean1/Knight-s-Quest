package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private double x, y;
    private final double width, height;
    private double velocityX = 0;
    private double velocityY = 0;
    private double gravity = 800;
    private double moveSpeed = 400;
    private double jumpStrength = -450;
    private boolean onGround = false;
    private boolean movingLeft = false;
    private boolean hasBoots = false;
    private boolean attackJustStarted = false;
    private double attackRadius = 110;

    private List<Image> idleFrames;
    private List<Image> runFrames;
    private List<Image> attackFrames;

    private int currentFrameIndex = 0;
    private boolean isIdle = true;
    private long lastFrameTime = 0;
    private long frameDuration = 100_000_000;
    private boolean isAttacking = false;
    private int attackFrameIndex = 0;
    private double attackAnimTime = 0;
    private double attackFrameDuration = 0.1;
    private int attackFrameCount = 4;

    public Player(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        loadSpriteSheets();
    }

    public void render(GraphicsContext gc, double cameraX) {
        List<Image> currentFrames;
        int frameIndex;
        double drawX = x - cameraX;
        double drawY = y - 54;
        double drawWidth = width * 2.5;
        double drawHeight = height * 2;

        if (isAttacking && attackFrames != null && !attackFrames.isEmpty()) {
            currentFrames = attackFrames;
            frameIndex = attackFrameIndex;
        } else if (!isIdle) {
            currentFrames = runFrames;
            frameIndex = currentFrameIndex;
        } else {
            currentFrames = idleFrames;
            frameIndex = currentFrameIndex;
        }
        if (!currentFrames.isEmpty()) {
            if (frameIndex >= currentFrames.size()) frameIndex = 0;
            Image currentFrame = currentFrames.get(frameIndex);
            if (!isIdle && movingLeft) {
                gc.save();
                gc.translate(drawX + drawWidth, 0);
                gc.scale(-1, 1);
                gc.drawImage(currentFrame, 0, drawY, drawWidth, drawHeight);
                gc.restore();
            } else {
                gc.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight);
            }
        }
    }

    public void update(double deltaTime, List<Platform> platforms, double canvasWidth, double canvasHeight, long now) {
        if (velocityX != 0) {
            isIdle = false;
        } else {
            isIdle = true;
        }

        // Updating of animation frames
        if (!isAttacking) {
            List<Image> currentFrames = isIdle ? idleFrames : runFrames;
            if (!currentFrames.isEmpty() && now - lastFrameTime > frameDuration) {
                currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
                lastFrameTime = now;
            }
        }

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
            if (checkPlatformCollision(platform, nextY)) break;
        }

        if (!onGround) {
            y = nextY;
        }

        if (y > canvasHeight) {
            if (!platforms.isEmpty()) {
                Platform startPlatform = platforms.get(0);
                x = startPlatform.getX() + 50;
                y = startPlatform.getY() - height;
                velocityY = 0;
                onGround = false;
            }
        }
        if (isAttacking) {
            attackAnimTime += deltaTime;
            if (attackAnimTime >= attackFrameDuration) {
                attackAnimTime = 0;
                attackFrameIndex++;
                if (attackFrameIndex >= attackFrameCount) {
                    isAttacking = false;
                    attackFrameIndex = 0;
                }
            }
        }
        attackJustStarted = false;
    }

    private void loadSpriteSheets() {
        Image idleSpriteSheet = new Image(getClass().getResource("/player/Idle.png").toExternalForm());
        idleFrames = new ArrayList<>();
        int frameWidth = 192;
        int frameHeight = 192;
        int idleFrameCount = 5;

        for (int i = 0; i < idleFrameCount; i++) {
            WritableImage frame = new WritableImage(idleSpriteSheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
            idleFrames.add(frame);
        }

        Image runSpriteSheet = new Image(getClass().getResource("/player/Run.png").toExternalForm());
        runFrames = new ArrayList<>();
        int runFrameCount = 6;

        for (int i = 0; i < runFrameCount; i++) {
            WritableImage frame = new WritableImage(runSpriteSheet.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
            runFrames.add(frame);
        }

        Image attackSheet = new Image(getClass().getResource("/player/Attack.png").toExternalForm());
        attackFrames = new ArrayList<>();
        int attackFrameWidth = 96;
        int attackFrameHeight = 96;
        int attackFrameCount = 4;
        for (int i = 0; i < attackFrameCount; i++) {
            WritableImage frame = new WritableImage(
                    attackSheet.getPixelReader(),
                    i * attackFrameWidth, 0, attackFrameWidth, attackFrameHeight
            );
            attackFrames.add(frame);
        }
    }

    public void restart(double x, double y) {
            this.x = x;
            this.y = y;
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


    public void moveLeft(double deltaTime) {
        velocityX = -getCurrentMoveSpeed();
        movingLeft = true;
    }
    public void moveRight(double deltaTime) {
        velocityX = getCurrentMoveSpeed();
        movingLeft = false;
    }

    public void stopHorizontalMovement() {
        velocityX = 0;
    }

    public void jump() {
        if (onGround) {
            velocityY = getJumpStrength();
            onGround = false;
        }
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void startAttack() {
        if (!isAttacking) {
            isAttacking = true;
            attackFrameIndex = 0;
            attackAnimTime = 0;
            attackJustStarted = true;
        }
    }
    public boolean isAttacking() { return isAttacking; }
    public boolean attackJustStarted() { return attackJustStarted; }
    public void resetAttackJustStarted() { attackJustStarted = false; }

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

    public void setHasBoots(boolean value) {
        this.hasBoots = value;
    }

    public boolean hasBoots() {
        return hasBoots;
    }

    private double getCurrentMoveSpeed() {
        return hasBoots ? moveSpeed * 1.5 : moveSpeed;
    }

    private double getJumpStrength() {return hasBoots ? jumpStrength * 1.25 : jumpStrength;}

    public double getAttackRadius() {return attackRadius;}
}