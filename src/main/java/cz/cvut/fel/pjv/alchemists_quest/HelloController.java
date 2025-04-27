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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;


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
    private Map<String, Integer> inventoryItems = new HashMap<>();
    private Set<KeyCode> activeKeys = new HashSet<>();
    private long lastUpdate = 0;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int PLATFORM_WIDTH = 200;
    private static final int PLATFORM_HEIGHT = 20;
    private static final int MAX_STACK_SIZE = 16;


    private double cameraX = 0;
    private double worldWidth = 2000;

    private void initializeInventory() {
        inventoryBox.getChildren().clear();
        for (int i = 0; i < 5; i++) {
            StackPane slot = new StackPane();

            ImageView imageView = new ImageView(new Image(getClass().getResource("/empty_slot.png").toExternalForm()));
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);

            Label countLabel = new Label();
            countLabel.setFont(new Font(14));
            countLabel.setTextFill(Color.WHITE);
            countLabel.setStyle("-fx-background-color: black; -fx-padding: 2px;");
            countLabel.setVisible(false);

            StackPane.setAlignment(countLabel, javafx.geometry.Pos.BOTTOM_RIGHT);

            slot.getChildren().addAll(imageView, countLabel);
            slot.setUserData("empty");
            inventoryBox.getChildren().add(slot);
        }
    }


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

        initializeInventory();

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();

        double canvasWidth = gameCanvas.getWidth();
        double canvasHeight = gameCanvas.getHeight();

        player = new Player(100, canvasHeight - PLAYER_HEIGHT - PLATFORM_HEIGHT - 50, PLAYER_WIDTH, PLAYER_HEIGHT);

        platforms.add(new Platform(50, canvasHeight - PLATFORM_HEIGHT - 20, PLATFORM_WIDTH, PLATFORM_HEIGHT));
        platforms.add(new Platform(200, 400, 150, 20));
        platforms.add(new Platform(500, 300, 200, 20));

        items.add(new Item(50, 100, "wood"));
        items.add(new Item(150, 180, "stone"));

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

    private void updateInventoryView(String newItemType) {
        for (Node node : inventoryBox.getChildren()) {
            if (node instanceof StackPane) {
                StackPane slot = (StackPane) node;
                ImageView imageView = (ImageView) slot.getChildren().get(0);
                Label countLabel = (Label) slot.getChildren().get(1);

                Object userData = slot.getUserData();

                if (newItemType.equals(userData)) {
                    int currentCount = Integer.parseInt(countLabel.getText());
                    if (currentCount < MAX_STACK_SIZE) {
                        currentCount++;
                        countLabel.setText(String.valueOf(currentCount));
                    } else {
                        infoLabel.setText("Player is not that strong!");
                    }
                    return;
                } else if (userData.equals("empty")) {
                    Image image = null;
                    switch (newItemType) {
                        case "wood":
                            image = new Image(getClass().getResource("/wood.png").toExternalForm());
                            break;
                        case "stone":
                            image = new Image(getClass().getResource("/stone.png").toExternalForm());
                            break;
                    }

                    if (image != null) {
                        imageView.setImage(image);
                        slot.setUserData(newItemType);
                        countLabel.setText("1");
                        countLabel.setVisible(true);
                    }
                    return;
                }
            }
        }
    }


    private void update(double deltaTime) {
        player.update(deltaTime, platforms, worldWidth, gameCanvas.getHeight());

        double canvasCenter = gameCanvas.getWidth() / 2;
        cameraX = player.getX() - canvasCenter + player.getWidth() / 2;

        cameraX = Math.max(0, Math.min(cameraX, worldWidth - gameCanvas.getWidth()));

        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                inventoryItems.put(item.getType(), inventoryItems.getOrDefault(item.getType(), 0) + 1);
                updateInventoryView(item.getType());
                iterator.remove();
            }
        }
    }


    private void render() {
        //background
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        //platform
        gc.setFill(Color.DARKGREEN);
        for (Platform platform : platforms) {
            gc.fillRect(platform.getX() - cameraX, platform.getY(), platform.getWidth(), platform.getHeight());
        }

        //player
        gc.setFill(Color.ORANGE);
        gc.fillRect(player.getX() - cameraX, player.getY(), player.getWidth(), player.getHeight());

        for (Item item : items) {
            item.render(gc, cameraX);
        }
    }
}
