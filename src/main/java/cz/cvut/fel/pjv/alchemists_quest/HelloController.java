package cz.cvut.fel.pjv.alchemists_quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.io.InputStreamReader;
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
    private List<NPC> npcs = new ArrayList<>();
    private boolean showDialog = false;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int MAX_STACK_SIZE = 16;

    private double cameraX = 0;
    private double worldWidth = 2000;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    activeKeys.add(event.getCode());
                    if (event.getCode() == KeyCode.C) {
                        craftingBox.setVisible(!craftingBox.isVisible());
                    }
                    if (event.getCode() == KeyCode.E) {
                        boolean nearAnyNPC = false;
                        for (NPC npc : npcs) {
                            if (npc.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                                nearAnyNPC = true;
                                break;
                            }
                        }
                        if (nearAnyNPC) {
                            showDialog = !showDialog;
                        }
                    }
                });
                newScene.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));
            }
        });

        gc = gameCanvas.getGraphicsContext2D();

        initializeInventory();

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();

        double canvasHeight = gameCanvas.getHeight();
        player = new Player(100, canvasHeight - PLAYER_HEIGHT - 50, PLAYER_WIDTH, PLAYER_HEIGHT);

        loadLevelFromJson("level1.json");

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

        if (showDialog) {
            boolean stillNearNPC = false;
            for (NPC npc : npcs) {
                if (npc.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    stillNearNPC = true;
                    break;
                }
            }
            if (!stillNearNPC) {
                showDialog = false;
            }
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

    private void render() {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.DARKGREEN);
        for (Platform platform : platforms) {
            gc.fillRect(platform.getX() - cameraX, platform.getY(), platform.getWidth(), platform.getHeight());
        }

        gc.setFill(Color.ORANGE);
        gc.fillRect(player.getX() - cameraX, player.getY(), player.getWidth(), player.getHeight());

        for (NPC npc : npcs) {
            npc.render(gc, cameraX);
        }

        if (showDialog) {
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(200, 100, 400, 100);
            gc.setFill(Color.WHITE);
            gc.setFont(new Font(24));
            gc.fillText("I can`t talk much yet(", 300, 160);
        }

        for (Item item : items) {
            item.render(gc, cameraX);
        }
    }

    private void loadLevelFromJson(String filename) {
        try {
            InputStream is = getClass().getResourceAsStream("/levels/" + filename);
            if (is == null) {
                System.err.println("Level file not found: " + filename);
                return;
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

            platforms.clear();
            npcs.clear();
            items.clear();

            JsonArray platformsArray = json.getAsJsonArray("platforms");
            for (int i = 0; i < platformsArray.size(); i++) {
                JsonObject obj = platformsArray.get(i).getAsJsonObject();
                platforms.add(new Platform(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("width").getAsDouble(),
                        obj.get("height").getAsDouble()
                ));
            }

            JsonArray npcsArray = json.getAsJsonArray("npcs");
            for (int i = 0; i < npcsArray.size(); i++) {
                JsonObject obj = npcsArray.get(i).getAsJsonObject();
                npcs.add(new NPC(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(), 40, 60));
            }

            JsonArray itemsArray = json.getAsJsonArray("items");
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject obj = itemsArray.get(i).getAsJsonObject();
                items.add(new Item(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("type").getAsString()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
