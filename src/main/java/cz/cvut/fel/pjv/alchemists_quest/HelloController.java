package cz.cvut.fel.pjv.alchemists_quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

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
    private VBox dialogBox;
    @FXML
    private Label dialogLabel;
    @FXML
    private VBox tradeBox;
    @FXML
    private Label dialogLabel1;

    private GraphicsContext gc;
    private Player player;
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Map<String, Integer> inventoryItems = new HashMap<>();
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private long lastUpdate = 0;
    private final List<NPC> npcs = new ArrayList<>();
    private boolean showDialog = false;
    private final List<Bush> bushes = new ArrayList<>();
    private Castle castle;
    private boolean gameWon = false;
    private boolean isDialogOpen = false;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int MAX_STACK_SIZE = 16;

    private double cameraX = 0;
    private double worldWidth = 2500;

    private void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(70), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0)); // Вернуть на место
        tt.play();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    activeKeys.add(event.getCode());
                    if (event.getCode() == KeyCode.E) {
                        boolean nearAnyNPC = false;
                        for (NPC npc : npcs) {
                            if (npc.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                                nearAnyNPC = true;
                                break;
                            }
                        }
                        for (Bush bush : bushes) {
                            if (bush.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight()) && bush.hasBerry()) {
                                bush.pickBerry();
                                inventoryItems.put("berry", inventoryItems.getOrDefault("berry", 0) + 1);
                                updateInventoryView("berry");
                                break;
                            }
                        }
                        if(isDialogOpen)
                        {
                            closeDialog();
                        }
                        else if(nearAnyNPC)
                        {
                            openDialog();
                        }
                    }
                    if (event.getCode() == KeyCode.ESCAPE) {
                        restartGame();
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

    @FXML
    private void handleTalk() {
        dialogLabel.setText("To complete the level u have to reach the castle!");
    }
    @FXML
    private void handleTrade() {
        tradeBox.setVisible(true);
        dialogBox.setVisible(false);
    }
    @FXML
    private void handleTradeClick(MouseEvent event) {
        Node source = (Node) event.getSource();
        String tradeType = (String) source.getUserData();
        tradeItems(tradeType);
    }

    @FXML
    private void backToMenu() {
        tradeBox.setVisible(false);
        dialogBox.setVisible(true);
    }

    private void openDialog() {
        isDialogOpen = true;
        dialogBox.setVisible(true);
        dialogLabel.setText("NPC: How can I help you?");
    }

    private void closeDialog() {
        isDialogOpen = false;
        dialogBox.setVisible(false);
    }

    private void tradeItems(String tradeType) {
        switch (tradeType) {
            case "berryToBoots":
                int berryCount = inventoryItems.getOrDefault("berry", 0);
                if (berryCount >= 2) {
                    inventoryItems.put("berry", berryCount - 2);
                    updateInventoryViewAfterRemoval("berry", 2);
                    inventoryItems.put("boots", inventoryItems.getOrDefault("boots", 0) + 1);
                    updateInventoryView("boots");
                    dialogLabel1.setText("You received Boots! Speed increased.");
                } else {
                    dialogLabel1.setText("Not enough berries for boots.");
                    shake(tradeBox);
                }
                break;
            case "stoneWoodToSword":
                int stoneCount = inventoryItems.getOrDefault("stone", 0);
                int woodCount = inventoryItems.getOrDefault("wood", 0);
                if (stoneCount >= 1 && woodCount >= 1) {
                    inventoryItems.put("stone", stoneCount - 1);
                    updateInventoryViewAfterRemoval("stone", 1);
                    inventoryItems.put("wood", woodCount - 1);
                    updateInventoryViewAfterRemoval("wood", 1);
                    inventoryItems.put("sword", inventoryItems.getOrDefault("sword", 0) + 1);
                    updateInventoryView("sword");
                    dialogLabel1.setText("You received a Sword!");
                } else {
                    dialogLabel1.setText("Not enough resources for sword.");
                    shake(tradeBox);
                }
                break;
        }
    }

    private void updateInventoryViewAfterRemoval(String itemType, int amount) {
        for (Node node : inventoryBox.getChildren()) {
            if (node instanceof StackPane slot && slot.getUserData().equals(itemType)) {
                Label countLabel = (Label) slot.getChildren().get(1);
                int currentCount = Integer.parseInt(countLabel.getText());
                currentCount -= amount;
                if (currentCount <= 0) {
                    ImageView imageView = (ImageView) slot.getChildren().get(0);
                    imageView.setImage(new Image(getClass().getResource("/empty_slot.png").toExternalForm()));
                    slot.setUserData("empty");
                    countLabel.setVisible(false);
                    countLabel.setText("");
                } else {
                    countLabel.setText(String.valueOf(currentCount));
                }
                break;
            }
        }
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
        player.update(deltaTime, platforms, worldWidth, gameCanvas.getHeight(), System.nanoTime());

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

        if (castle != null && castle.intersects(player)) {
            gameWon = true;
        }
        if (showDialog) {
            boolean stillNearNPC = false;
            for (NPC npc : npcs) {
                if (npc.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    stillNearNPC = true;
                    break;
                }
            }
            for (Bush bush : bushes) {
                if (bush.hasBerry() && bush.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    bush.pickBerry();
                    inventoryItems.put("berry", inventoryItems.getOrDefault("berry", 0) + 1);
                    updateInventoryView("berry");
                    break;
                }
            }
            if (castle != null && castle.intersects(player)) {
                gameWon = true;
            }
            if (!stillNearNPC) {
                showDialog = false;
            }
        }
    }
    private void showVictoryScreen() {
        gc.setFill(new Color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        infoLabel.setText("Press ESC to restart");

        Image winImage = new Image(getClass().getResourceAsStream("/win.png"));

        double imageX = (gameCanvas.getWidth() - winImage.getWidth()) / 2;
        double imageY = (gameCanvas.getHeight() - winImage.getHeight()) / 2;

        gc.drawImage(winImage, imageX, imageY);
    }
    private void restartGame() {
        gameWon = false;
        infoLabel.setText("Info: Press A/D to move, SPACE to jump, C to Craft, E to interract");
        inventoryItems.clear();
        initializeInventory();
        loadLevelFromJson("level1.json");
        double canvasHeight = gameCanvas.getHeight();
        player.restart(100, canvasHeight - PLAYER_HEIGHT - 50);
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
                        case "berry":
                            image = new Image(getClass().getResource("/berry.png").toExternalForm());
                            break;
                        case "boots":
                            image = new Image(getClass().getResource("/boots.png").toExternalForm());
                            break;
                        case "sword":
                            image = new Image(getClass().getResource("/sword.png").toExternalForm());
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

        player.render(gc, cameraX);

        for (NPC npc : npcs) {
            npc.render(gc, cameraX);
        }

        for (Item item : items) {
            item.render(gc, cameraX);
        }

        for (Bush bush : bushes) {
            bush.render(gc, cameraX);
        }
        if (castle != null) {
            castle.render(gc, cameraX);
        }
        if (gameWon) {
            showVictoryScreen();
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

            JsonArray npcArray = json.getAsJsonArray("npcs");
            for (JsonElement nElem : npcArray) {
                JsonObject obj = nElem.getAsJsonObject();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                Image image = new Image(getClass().getResource("/npc.png").toExternalForm());
                npcs.add(new NPC(x, y, image));
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

            JsonArray bushArray = json.getAsJsonArray("bushes");
            for (JsonElement bElem : bushArray) {
                JsonObject b = bElem.getAsJsonObject();
                double x = b.get("x").getAsDouble();
                double y = b.get("y").getAsDouble();
                bushes.add(new Bush(x, y));
            }
            if (json.has("castle")) {
                JsonObject castleObj = json.getAsJsonObject("castle");
                double cx = castleObj.get("x").getAsDouble();
                double cy = castleObj.get("y").getAsDouble();
                castle = new Castle(cx, cy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}