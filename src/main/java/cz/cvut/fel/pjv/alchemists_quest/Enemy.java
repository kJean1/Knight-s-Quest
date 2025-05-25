package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class Enemy {
    public static final Logger GAME_LOGGER = Logger.getLogger("KnightsQuestLogger");
    
    private double x, y;
    private final double width, height;
    private double velocityX = 160;
    private double velocityY = 0;
    private double gravity = 800;
    private boolean onGround = false;
    private boolean movingRight = true;

    // Patrolling state
    private boolean patrolCurrentPlatform = false;
    private Platform currentPlatform = null;

    // Run Animation
    private List<Image> runFrames;
    private int currentFrameIndex = 0;
    private long lastFrameTime = 0;
    private long frameDuration = 90_000_000;

    // Attack Animation
    private List<Image> attackFrames;
    private int attackFrameIndex = 0;
    private double attackAnimTime = 0;
    private final double attackFrameDuration = 0.10;
    private final int attackFrameCount = 4;
    private boolean isAttacking = false;
    private double attackCooldown = 0;

    // Death Animation
    private List<Image> deadFrames;
    private int deadFrameIndex = 0;
    private double deadAnimTime = 0;
    private final double deadFrameDuration = 0.13;
    private final int deadFrameCount = 4;
    private boolean isDead = false;
    private boolean shouldBeRemoved = false;

    // Jump Animation
    private List<Image> jumpFrames;
    private int jumpFrameIndex = 0;
    private double jumpAnimTime = 0;
    private final double jumpFrameDuration = 0.08;
    private final int jumpFrameCount = 8;
    private boolean isJumping = false;
    private double jumpStartY = 0;
    private double jumpTargetX = 0;
    private double lastJumpTime = 0;
    private double jumpCooldown = 0.2;

    // Attack radius
    private static final double ATTACK_RADIUS = 100;

    public Enemy(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        loadSpriteSheets();
    }

    private void loadSpriteSheets() {
        // Run
        Image runSpriteSheet = new Image(getClass().getResource("/enemy/Run.png").toExternalForm());
        runFrames = new ArrayList<>();
        int runFrameWidth = 96;
        int runFrameCount = 6;
        for (int i = 0; i < runFrameCount; i++) {
            WritableImage frame = new WritableImage(runSpriteSheet.getPixelReader(), i * runFrameWidth, 0, runFrameWidth, 96);
            runFrames.add(frame);
        }
        // Attack
        Image attackSpriteSheet = new Image(getClass().getResource("/enemy/Attack.png").toExternalForm());
        attackFrames = new ArrayList<>();
        int attackFrameWidth = 96;
        for (int i = 0; i < attackFrameCount; i++) {
            WritableImage frame = new WritableImage(attackSpriteSheet.getPixelReader(), i * attackFrameWidth, 0, attackFrameWidth, 96);
            attackFrames.add(frame);
        }
        // Dead
        Image deadSpriteSheet = new Image(getClass().getResource("/enemy/Dead.png").toExternalForm());
        deadFrames = new ArrayList<>();
        int deadFrameWidth = 96;
        for (int i = 0; i < deadFrameCount; i++) {
            WritableImage frame = new WritableImage(deadSpriteSheet.getPixelReader(), i * deadFrameWidth, 0, deadFrameWidth, 96);
            deadFrames.add(frame);
        }
        // Jump
        Image jumpSpriteSheet = new Image(getClass().getResource("/enemy/Jump.png").toExternalForm());
        jumpFrames = new ArrayList<>();
        int jumpFrameWidth = 96;
        for (int i = 0; i < jumpFrameCount; i++) {
            WritableImage frame = new WritableImage(jumpSpriteSheet.getPixelReader(), i * jumpFrameWidth, 0, jumpFrameWidth, 96);
            jumpFrames.add(frame);
        }
    }

    // Search for Platforms for Jump (by X and Y)
    private Platform findPlatformForJump(List<Platform> platforms, boolean toRight) {
        double searchStart = toRight ? x + width : x;
        double bestDist = Double.MAX_VALUE;
        Platform best = null;
        for (Platform p : platforms) {
            double px = toRight ? p.getX() : p.getX() + p.getWidth();
            double dx = Math.abs(px - searchStart);
            double dy = p.getY() - (y + height);
            // Condition for the maximum distance in X and according to Y, and to make the platform in front
            if (dx > 35 && dx < 270 && Math.abs(dy) < 65) {
                if ((toRight && px > searchStart) || (!toRight && px < searchStart)) {
                    double dist = Math.hypot(dx, dy);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    // Find the platform on which the enemy is now standing (for patrolling)
    private Platform getStoodPlatform(List<Platform> platforms) {
        for (Platform p : platforms) {
            if (x + width > p.getX() && x < p.getX() + p.getWidth()) {
                if (Math.abs((y + height) - p.getY()) < 2) {
                    return p;
                }
            }
        }
        return null;
    }

    public void update(double deltaTime, List<Platform> platforms, Player player, long now) {
        if (shouldBeRemoved) return;

        if (isDead) {
            // Animation of death
            deadAnimTime += deltaTime;
            if (deadAnimTime >= deadFrameDuration) {
                deadAnimTime = 0;
                deadFrameIndex++;
                if (deadFrameIndex >= deadFrameCount) {
                    shouldBeRemoved = true;
                }
            }
            return;
        }

        double playerCenterX = player.getX() + player.getWidth() / 2;
        double enemyCenterX = x + width / 2;
        double playerDistX = playerCenterX - enemyCenterX;
        double absDistX = Math.abs(playerDistX);

        // Define the current patrol platform
        if (onGround) {
            currentPlatform = getStoodPlatform(platforms);
        }

        // Enemy`s AI
        boolean tryFollowPlayer = false;
        if (absDistX <= 290 && Math.abs(y - player.getY()) < 120) {
            tryFollowPlayer = true;
        }

        // an enemy can attack if a player in an enlarged radius of attack
        if (absDistX <= ATTACK_RADIUS && Math.abs(y - player.getY()) < 80 && attackCooldown <= 0) {
            if (!isAttacking) {
                isAttacking = true;
                attackAnimTime = 0;
                attackFrameIndex = 0;
            }
        }

        if (isAttacking) {
            attackAnimTime += deltaTime;
            if (attackAnimTime >= attackFrameDuration) {
                attackAnimTime = 0;
                attackFrameIndex++;
                if (attackFrameIndex >= attackFrameCount) {
                    isAttacking = false;
                    attackCooldown = 0.7;
                    attackFrameIndex = 0;
                }
            }
        } else {
            if (attackCooldown > 0) attackCooldown -= deltaTime;
        }

        // If the enemy tries to pursue the player and does not patrol, chooses the direction
        if (tryFollowPlayer && !patrolCurrentPlatform) {
            movingRight = playerDistX > 0;
        }

        // jump through the abyss to another platform
        boolean triedToJump = false;
        if (!isJumping && onGround && !patrolCurrentPlatform && (now - lastJumpTime) > (jumpCooldown * 1_000_000_000L)) {
            double checkX = movingRight ? x + width + velocityX * 0.18 : x - velocityX * 0.18;
            boolean groundAhead = false;
            for (Platform p : platforms) {
                if (checkX + 5 > p.getX() && checkX < p.getX() + p.getWidth()) {
                    if (y + height + 2 >= p.getY() && y + height + 2 <= p.getY() + 10) {
                        groundAhead = true;
                        break;
                    }
                }
            }
            // If there is no land ahead, we are looking for a jump platform
            if (!groundAhead) {
                Platform target = findPlatformForJump(platforms, movingRight);
                if (target != null) {
                    double tx = movingRight ? target.getX() + 8 : target.getX() + target.getWidth() - width - 8;
                    double dx = tx - x;
                    double dy = target.getY() - y;
                    if (Math.abs(dx) > 35 && Math.abs(dx) < 270 && Math.abs(dy) < 65) {
                        isJumping = true;
                        jumpFrameIndex = 0;
                        jumpAnimTime = 0;
                        jumpStartY = y;
                        jumpTargetX = tx;
                        velocityY = -470;
                        lastJumpTime = now;
                        patrolCurrentPlatform = false;
                    }
                } else {
                    // cannot jump - begins to patrol his platform
                    patrolCurrentPlatform = true;
                }
                triedToJump = true;
            } else {
                patrolCurrentPlatform = false;
            }
        }

        // patrol of the platform
        if (patrolCurrentPlatform && currentPlatform != null) {
            if (movingRight && x + width + velocityX * deltaTime > currentPlatform.getX() + currentPlatform.getWidth()) {
                movingRight = false;
            } else if (!movingRight && x - velocityX * deltaTime < currentPlatform.getX()) {
                movingRight = true;
            }
        }

        // Gravity and movement
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

        // Movement on x
        double vx = movingRight ? velocityX : -velocityX;
        if (isJumping) {
            double jumpDir = (jumpTargetX - x);
            if (Math.abs(jumpDir) > 10) {
                vx = Math.signum(jumpDir) * (velocityX + 70);
            }
        }
        x += vx * deltaTime;

        if (!onGround) {
            y = nextY;
        } else if (isJumping) {
            isJumping = false;
        }

        // Animation of the jump
        if (isJumping && !jumpFrames.isEmpty()) {
            jumpAnimTime += deltaTime;
            if (jumpAnimTime >= jumpFrameDuration) {
                jumpAnimTime = 0;
                jumpFrameIndex = (jumpFrameIndex + 1) % jumpFrames.size();
            }
        }

        // running animation (if it does not attack/does not jump)
        if (!runFrames.isEmpty() && now - lastFrameTime > frameDuration && !isAttacking && !isJumping) {
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

    public boolean tryAttackPlayer(Player player) {
        if (isAttacking && attackFrameIndex == 2) {
            double enemyCenter = x + width / 2;
            double playerCenter = player.getX() + player.getWidth() / 2;
            double dist = Math.abs(enemyCenter - playerCenter);
            if (dist <= ATTACK_RADIUS && Math.abs(y - player.getY()) < 80) {
                return true;
            }
        }
        return false;
    }

    public void die() {
        if (!isDead) {
            isDead = true;
            deadAnimTime = 0;
            deadFrameIndex = 0;
        }
    }

    public boolean isDead() { return isDead; }
    public boolean shouldBeRemoved() { return shouldBeRemoved; }

    public void render(GraphicsContext gc, double cameraX) {
        Image currentFrame = null;
        double drawX = x - cameraX;
        double drawY = y - 54;
        double drawWidth = width * 2;
        double drawHeight = height * 2;

        if (isDead && !deadFrames.isEmpty()) {
            int idx = Math.min(deadFrameIndex, deadFrames.size() - 1);
            currentFrame = deadFrames.get(idx);
        } else if (isAttacking && !attackFrames.isEmpty()) {
            int idx = Math.min(attackFrameIndex, attackFrames.size() - 1);
            currentFrame = attackFrames.get(idx);
        } else if (isJumping && !jumpFrames.isEmpty()) {
            int idx = Math.min(jumpFrameIndex, jumpFrames.size() - 1);
            currentFrame = jumpFrames.get(idx);
        } else if (!runFrames.isEmpty()) {
            currentFrame = runFrames.get(currentFrameIndex);
        }

        if (currentFrame != null) {
            if (!movingRight) {
                gc.save();
                gc.translate(drawX + drawWidth, 0);
                gc.scale(-1, 1);
                gc.drawImage(currentFrame, 0, drawY, drawWidth, drawHeight);
                gc.restore();
            } else {
                gc.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight);
            }
        } else {
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(drawX, drawY, drawWidth, drawHeight);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}