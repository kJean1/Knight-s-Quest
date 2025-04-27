package cz.cvut.fel.pjv.alchemists_quest;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class HelloController implements Initializable {

    @FXML
    private Canvas gameCanvas;
    @FXML
    private Label infoLabel;
    @FXML
    private HBox inventoryBox;
    @FXML
    private GridPane craftingGrid;
    @FXML
    private VBox craftingBox;
    @FXML
    private Button craftButton;
    @FXML
    private Label craftResultLabel;

    private GraphicsContext gc;
    private Player player;
    private List<Platform> platforms = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private List<String> inventoryItems = new ArrayList<>();
    private Set<KeyCode> activeKeys = new HashSet<>();
    private long lastUpdate = 0;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int PLATFORM_WIDTH = 200;
    private static final int PLATFORM_HEIGHT = 20;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    activeKeys.add(event.getCode());
                    if (event.getCode() == KeyCode.C) {
                        craftingBox.setVisible(!craftingBox.isVisible());
                    }
                });
                newScene.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));
            }
        });

        gc = gameCanvas.getGraphicsContext2D();

        // Установить пустую картинку для всех слотов
        for (Node node : inventoryBox.getChildren()) {
            if (node instanceof ImageView) {
                ImageView slot = (ImageView) node;
                slot.setImage(new Image(getClass().getResource("/empty_slot.png").toExternalForm()));
                slot.setFitWidth(50);
                slot.setFitHeight(50);
            }
        }

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();

        double canvasWidth = gameCanvas.getWidth();
        double canvasHeight = gameCanvas.getHeight();

        player = new Player(100, canvasHeight - PLAYER_HEIGHT - PLATFORM_HEIGHT - 50, PLAYER_WIDTH, PLAYER_HEIGHT);

        platforms.add(new Platform(50, canvasHeight - PLATFORM_HEIGHT - 20, PLATFORM_WIDTH, PLATFORM_HEIGHT));
        platforms.add(new Platform(200, 400, 150, 20));
        platforms.add(new Platform(500, 300, 200, 20));

        items.add(new Item(100, 100, "wood"));

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                elapsedSeconds = Math.min(elapsedSeconds, 0.1);

                handleInput(elapsedSeconds);
                update(elapsedSeconds);
                render();
            }
        };
        gameLoop.start();
    }

    private void handleInput(double deltaTime) {
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) {
            player.moveLeft(deltaTime);
        } else if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) {
            player.moveRight(deltaTime);
        } else {
            player.stopHorizontalMovement();
        }
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.SPACE)) {
            if (player.isOnGround()) {
                player.jump();
            }
            activeKeys.remove(KeyCode.W);
            activeKeys.remove(KeyCode.SPACE);
        }
    }

    private void updateInventoryView() {
        for (Node node : inventoryBox.getChildren()) {
            if (node instanceof ImageView) {
                ImageView slot = (ImageView) node;
                if (slot.getImage().getUrl().contains("empty_slot.png")) {
                    String newItemType = inventoryItems.get(inventoryItems.size() - 1);
                    Image image = null;
                    switch (newItemType) {
                        case "wood":
                            image = new Image(getClass().getResource("/wood.png").toExternalForm());
                            break;
                        // другие предметы
                    }
                    if (image != null) {
                        slot.setImage(image);
                    }
                    break;
                }
            }
        }
    }

    private void update(double deltaTime) {
        player.update(deltaTime, platforms, gameCanvas.getWidth(), gameCanvas.getHeight());

        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                inventoryItems.add(item.getType());
                updateInventoryView();
                iterator.remove();
            }
        }
    }

    private void render() {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.DARKGREEN);
        for (Platform platform : platforms) {
            gc.fillRect(platform.getX(), platform.getY(), platform.getWidth(), platform.getHeight());
        }

        gc.setFill(Color.ORANGE);
        gc.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        for (Item item : items) {
            item.render(gc);
        }
    }
}
