package cz.cvut.fel.pjv.alchemists_quest;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.List;

public class NPC {
    private final double x, y;
    private List<Image> idleFrames;
    private int currentFrameIndex = 0;
    private long lastFrameTime = 0;
    private final long frameDuration = 200_000_000;

    private static final double WIDTH = 140;
    private static final double HEIGHT = 160;

    public NPC(double x, double y) {
        this.x = x;
        this.y = y;
        loadIdleFrames();
    }

    private void loadIdleFrames() {
            Image idleSpriteSheet = new Image(getClass().getResource("/npc/Idle.png").toExternalForm());
            idleFrames = new ArrayList<>();
            int frameWidth = 96;
            int frameHeight = 96;
            int idleFrameCount = 6;

            for (int i = 0; i < idleFrameCount; i++) {
                WritableImage frame = new WritableImage(
                        idleSpriteSheet.getPixelReader(),
                        i * frameWidth, 0, frameWidth, frameHeight
                );
                idleFrames.add(frame);
            }
    }

    public void update(long now) {
        if (idleFrames == null || idleFrames.isEmpty()) return;
        if (now - lastFrameTime > frameDuration) {
            currentFrameIndex = (currentFrameIndex + 1) % idleFrames.size();
            lastFrameTime = now;
        }
    }

    public void render(GraphicsContext gc, double cameraX) {
        if (idleFrames != null && !idleFrames.isEmpty()) {
            Image currentFrame = idleFrames.get(currentFrameIndex);
            gc.drawImage(currentFrame, x - cameraX, y-66, WIDTH, HEIGHT);
        }
    }

    public boolean isNear(double px, double py) {
        return Math.abs(px - x) < 50 && Math.abs(py - y) < 50;
    }
}