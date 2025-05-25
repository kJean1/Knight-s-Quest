package cz.cvut.fel.pjv.alchemists_quest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    @FXML
    private VBox victoryMenuBox;
    @FXML
    private VBox pauseMenuBox;
    @FXML
    private Button nextLevelButton;
    @FXML
    private Button restartLevelButtonVictory;
    @FXML
    private Button restartLevelButtonPause;
    @FXML
    private Button resumeButton;
    @FXML
    private Button selectLevelButtonVictory;
    @FXML
    private Button selectLevelButtonPause;
    @FXML
    private ComboBox<String> levelComboBoxVictory;
    @FXML
    private ComboBox<String> levelComboBoxPause;

    // Main menu fields
    @FXML private VBox mainMenuBox;
    @FXML private Button playButton;
    @FXML private ComboBox<String> levelComboBox;
    @FXML private CheckBox loggingCheckBox;
    @FXML private ImageView logoView;
    @FXML private Button loadSaveButton;
    @FXML private ComboBox<String> saveComboBox;

    // Logger
    public static final Logger GAME_LOGGER = Logger.getLogger("KnightsQuestLogger");
    public static boolean loggingEnabled = false;

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
    private boolean paused = false;
    private boolean isDialogOpen = false;
    private final List<Enemy> enemies = new ArrayList<>();
    private List<String> levels;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int MAX_STACK_SIZE = 16;
    private static final int INVENTORY_SIZE = 5;

    private int selectedInventoryIndex = 0;
    private double cameraX = 0;
    private double worldWidth = 2500;
    private String currentLevel = "level1.json";

    // Lists for collected items and bushes
    private final List<Point2D> collectedItemPositions = new ArrayList<>();
    private final List<Point2D> collectedBushPositions = new ArrayList<>();

    private void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(70), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // --- LOGGER setup ---
        setupLogger();

        // --- Main menu logic ---
        if (mainMenuBox != null) {
            // Логотип
            Image logoImg = new Image(getClass().getResource("/logo.png").toExternalForm());
            logoView.setImage(logoImg);
            inventoryBox.setVisible(false);
            inventoryBox.setManaged(false);
            gameCanvas.setDisable(true);
            infoLabel.setVisible(false);

            levels = getAllLevelFiles();
            levelComboBox.getItems().setAll(levels);
            levelComboBox.setValue(levels.contains("level1.json") ? "level1.json" : levels.get(0));

            loggingCheckBox.setSelected(loggingEnabled);
            loggingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                loggingEnabled = newVal;
                if (loggingEnabled) {
                    GAME_LOGGER.setLevel(Level.INFO);
                    GAME_LOGGER.info("Logging enabled");
                } else {
                    GAME_LOGGER.setLevel(Level.OFF);
                }
            });

            playButton.setOnAction(e -> showGame());

            // SAVE
            updateSaveComboBox();
            loadSaveButton.setOnAction(e -> loadSelectedSave());
        }

        if (levels == null) levels = getAllLevelFiles();
        levelComboBoxVictory.getItems().setAll(levels);
        levelComboBoxPause.getItems().setAll(levels);
        victoryMenuBox.setVisible(false);
        pauseMenuBox.setVisible(false);

        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    activeKeys.add(event.getCode());
                    if (event.getCode() == KeyCode.E) {
                        boolean nearAnyNPC = false;
                        for (NPC npc : npcs) {
                            if (npc.isNear(player.getX(), player.getY())) {
                                nearAnyNPC = true;
                                break;
                            }
                        }
                        for (Bush bush : bushes) {
                            if (bush.isNear(player.getX(), player.getY(), player.getWidth(), player.getHeight()) && bush.hasBerry()) {
                                bush.pickBerry();
                                inventoryItems.put("berry", inventoryItems.getOrDefault("berry", 0) + 1);
                                collectedBushPositions.add(new Point2D(bush.getX(), bush.getY()));
                                renderFullInventory();
                                break;
                            }
                        }
                        if (isDialogOpen) {
                            closeDialog();
                        } else if (nearAnyNPC) {
                            openDialog();
                        }
                        if(tradeBox.getScene() != null) {
                            tradeBox.setVisible(false);
                        }
                    }
                    if (event.getCode() == KeyCode.ESCAPE) {
                        if (paused) {
                            hideMenus();
                        } else {
                            showPauseMenu();
                        }
                    }
                });
                gameCanvas.setOnMousePressed(event -> {
                    if (event.isPrimaryButtonDown()) {
                        String selectedType = getSelectedInventoryItemType();
                        if ("sword".equals(selectedType)) {
                            player.startAttack();
                        }
                    }
                });
                gameCanvas.setOnScroll(event -> {
                    int inventorySize = inventoryBox.getChildren().size();
                    if (event.getDeltaY() > 0) {
                        selectedInventoryIndex = (selectedInventoryIndex - 1 + inventorySize) % inventorySize;
                    } else if (event.getDeltaY() < 0) {
                        selectedInventoryIndex = (selectedInventoryIndex + 1) % inventorySize;
                    }
                    updateInventorySelection();
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

        if (mainMenuBox != null && mainMenuBox.isVisible()) {
            inventoryBox.setVisible(false);
            inventoryBox.setManaged(false);
            gameCanvas.setDisable(true);
        } else {
            inventoryBox.setVisible(true);
            inventoryBox.setManaged(true);
        }

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (mainMenuBox != null && mainMenuBox.isVisible()) {
                    return;
                }
                if (paused) {
                    render();
                    return;
                }
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

    private void showGame() {
        if (mainMenuBox != null) {
            mainMenuBox.setVisible(false);
            mainMenuBox.setManaged(false);
            gameCanvas.setDisable(false);
            inventoryBox.setVisible(true);
            inventoryBox.setManaged(true);
            infoLabel.setVisible(true);

            String chosenLevel = levelComboBox.getValue();
            if (chosenLevel != null && !chosenLevel.isEmpty()) {
                currentLevel = chosenLevel;
            }
            loadLevelFromJson(currentLevel);
            gameCanvas.setFocusTraversable(true);
            gameCanvas.requestFocus();
            if (loggingEnabled) GAME_LOGGER.info("Game started: " + currentLevel);
        }
    }

    private void loadSelectedSave() {
        String saveFileName = saveComboBox.getValue();
        inventoryBox.setVisible(true);
        inventoryBox.setManaged(true);
        infoLabel.setVisible(true);

        if (saveFileName == null || saveFileName.isEmpty()) {
            GAME_LOGGER.warning("No save selected!");
            return;
        }
        String levelFromSave = saveFileName.replace("_save.json", ".json");
        currentLevel = levelFromSave;
        boolean loaded = loadProgress(currentLevel);
        if (loaded) {
            mainMenuBox.setVisible(false);
            mainMenuBox.setManaged(false);
            gameCanvas.setDisable(false);
            gameCanvas.setFocusTraversable(true);
            gameCanvas.requestFocus();
            GAME_LOGGER.info("Loaded save: " + saveFileName);
        } else {
            GAME_LOGGER.warning("Save file could not be loaded: " + saveFileName);
        }
    }

    private void updateSaveComboBox() {
        File savesDir = new File("saves");
        List<String> saves = new ArrayList<>();
        if (savesDir.exists() && savesDir.isDirectory()) {
            File[] files = savesDir.listFiles((dir, name) -> name.endsWith("_save.json"));
            if (files != null) {
                for (File f : files) {
                    saves.add(f.getName());
                }
            }
        }
        saves.sort(Comparator.naturalOrder());
        saveComboBox.getItems().setAll(saves);
        if (!saves.isEmpty()) {
            saveComboBox.setValue(saves.get(0));
        }
    }

    private static void setupLogger() {
        if (GAME_LOGGER.getHandlers().length == 0) {
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            GAME_LOGGER.setUseParentHandlers(false);
            GAME_LOGGER.addHandler(consoleHandler);
        }
        GAME_LOGGER.setLevel(loggingEnabled ? Level.INFO : Level.OFF);
    }

    @FXML
    private void onNextLevel() {
        int currentIdx = levels.indexOf(currentLevel);
        if (currentIdx < levels.size() - 1) {
            currentLevel = levels.get(currentIdx + 1);
            levelComboBoxVictory.setValue(currentLevel);
            levelComboBoxPause.setValue(currentLevel);
            restartGame();
        }
    }

    @FXML
    private void onRestartLevel() {
        hideMenus();
        restartGame();
    }

    @FXML
    private void onSaveProgress() {
        saveProgress(currentLevel, inventoryItems, player.getX(), player.getY());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Save");
        alert.setHeaderText(null);
        alert.setContentText("Saved successfully!");
        alert.showAndWait();
        updateSaveComboBox();
    }

    private void saveProgress(String currentLevel, Map<String, Integer> inventory, double playerX, double playerY) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("level", currentLevel);
            root.putPOJO("inventory", inventory);

            ObjectNode position = mapper.createObjectNode();
            position.put("x", playerX);
            position.put("y", playerY);
            root.set("playerPosition", position);

            ArrayList<ObjectNode> collectedItemsArr = new ArrayList<>();
            for (Point2D pt : collectedItemPositions) {
                ObjectNode node = mapper.createObjectNode();
                node.put("x", pt.getX());
                node.put("y", pt.getY());
                collectedItemsArr.add(node);
            }
            root.putPOJO("collectedItems", collectedItemsArr);

            ArrayList<ObjectNode> collectedBushesArr = new ArrayList<>();
            for (Point2D pt : collectedBushPositions) {
                ObjectNode node = mapper.createObjectNode();
                node.put("x", pt.getX());
                node.put("y", pt.getY());
                collectedBushesArr.add(node);
            }
            root.putPOJO("collectedBushes", collectedBushesArr);

            File dir = new File("saves");
            if (!dir.exists()) dir.mkdirs();
            String savePath = "saves/" + currentLevel.replace(".json", "_save.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(savePath), root);

            GAME_LOGGER.info("Progress saved!");
        } catch (Exception e) {
            GAME_LOGGER.log(Level.SEVERE, "Error saving progress!", e);
        }
    }

    private boolean loadProgress(String levelName) {
        try {
            File saveFile = new File("saves/" + levelName.replace(".json", "_save.json"));
            if (!saveFile.exists()) {
                GAME_LOGGER.info("No save found for level: " + levelName);
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(saveFile);

            String savedLevelName = root.get("level").asText();

            loadLevelFromJson(savedLevelName);
            currentLevel = savedLevelName;

            JsonNode invNode = root.get("inventory");
            inventoryItems.clear();
            Iterator<Map.Entry<String, JsonNode>> fields = invNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                int value = entry.getValue().asInt();
                if (value > 0) {
                    inventoryItems.put(entry.getKey(), value);
                }
            }
            renderFullInventory();

            JsonNode pos = root.get("playerPosition");
            double x = pos.get("x").asDouble();
            double y = pos.get("y").asDouble();
            player.restart(x, y);

            collectedItemPositions.clear();
            collectedBushPositions.clear();

            if (root.has("collectedItems")) {
                for (JsonNode node : root.get("collectedItems")) {
                    double ix = node.get("x").asDouble();
                    double iy = node.get("y").asDouble();
                    Point2D pt = new Point2D(ix, iy);
                    collectedItemPositions.add(pt);
                    items.removeIf(item -> Math.abs(item.getX() - ix) < 0.01 && Math.abs(item.getY() - iy) < 0.01);
                }
            }

            if (root.has("collectedBushes")) {
                for (JsonNode node : root.get("collectedBushes")) {
                    double bx = node.get("x").asDouble();
                    double by = node.get("y").asDouble();
                    Point2D pt = new Point2D(bx, by);
                    collectedBushPositions.add(pt);
                    for (Bush bush : bushes) {
                        if (Math.abs(bush.getX() - bx) < 0.01 && Math.abs(bush.getY() - by) < 0.01) {
                            bush.pickBerry();
                        }
                    }
                }
            }

            GAME_LOGGER.info("Progress loaded!");
            return true;
        } catch (Exception e) {
            GAME_LOGGER.log(Level.SEVERE, "Error loading progress!", e);
            return false;
        }
    }

    private void renderFullInventory() {
        for (Node node : inventoryBox.getChildren()) {
            if (node instanceof StackPane slot) {
                ImageView imageView = (ImageView) slot.getChildren().get(0);
                Label countLabel = (Label) slot.getChildren().get(1);
                imageView.setImage(new Image(getClass().getResource("/empty_slot.png").toExternalForm()));
                slot.setUserData("empty");
                countLabel.setVisible(false);
                countLabel.setText("");
            }
        }
        int slotIdx = 0;
        for (Map.Entry<String, Integer> entry : inventoryItems.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            if (count > 0 && slotIdx < inventoryBox.getChildren().size()) {
                StackPane slot = (StackPane) inventoryBox.getChildren().get(slotIdx);
                ImageView imageView = (ImageView) slot.getChildren().get(0);
                Label countLabel = (Label) slot.getChildren().get(1);

                Image image = null;
                switch (type) {
                    case "wood": image = new Image(getClass().getResource("/wood.png").toExternalForm()); break;
                    case "stone": image = new Image(getClass().getResource("/stone.png").toExternalForm()); break;
                    case "berry": image = new Image(getClass().getResource("/berry.png").toExternalForm()); break;
                    case "boots": image = new Image(getClass().getResource("/boots.png").toExternalForm()); break;
                    case "sword": image = new Image(getClass().getResource("/sword.png").toExternalForm()); break;
                }
                if (image != null) {
                    imageView.setImage(image);
                    slot.setUserData(type);
                    countLabel.setText(String.valueOf(count));
                    countLabel.setVisible(true);
                }
                slotIdx++;
            }
        }
        updateInventorySelection();
    }

    @FXML
    private void onSelectLevel(ActionEvent event) {
        Object source = event.getSource();
        String selectedLevel = null;

        if (source == selectLevelButtonVictory) {
            selectedLevel = levelComboBoxVictory.getValue();
            levelComboBoxPause.setValue(selectedLevel);
        } else if (source == selectLevelButtonPause) {
            selectedLevel = levelComboBoxPause.getValue();
            levelComboBoxVictory.setValue(selectedLevel);
        }

        if (selectedLevel != null && !selectedLevel.equals(currentLevel)) {
            currentLevel = selectedLevel;
            boolean loaded = loadProgress(currentLevel);
            if (!loaded) {
                restartGame();
            }
        }
        hideMenus();
    }

    @FXML
    private void onResume() {
        hideMenus();
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
        if (tradeType.equals("berryToBoots")) {
            int berryCount = inventoryItems.getOrDefault("berry", 0);
            if (berryCount >= 2) {
                inventoryItems.put("berry", berryCount - 2);
                updateInventoryViewAfterRemoval("berry", 2);
                inventoryItems.put("boots", inventoryItems.getOrDefault("boots", 0) + 1);
                renderFullInventory();
                dialogLabel1.setText("You received Boots! Speed and jump strength increased.");
            } else {
                dialogLabel1.setText("Not enough berries for boots.");
                shake(tradeBox);
            }
        } else if (tradeType.equals("stoneWoodToSword")) {
            int stoneCount = inventoryItems.getOrDefault("stone", 0);
            int woodCount = inventoryItems.getOrDefault("wood", 0);
            if (stoneCount >= 1 && woodCount >= 1) {
                inventoryItems.put("stone", stoneCount - 1);
                inventoryItems.put("wood", woodCount - 1);
                inventoryItems.put("sword", inventoryItems.getOrDefault("sword", 0) + 1);
                renderFullInventory();
                dialogLabel1.setText("You received a Sword!");
            } else {
                dialogLabel1.setText("Not enough resources for sword.");
                shake(tradeBox);
            }
        }
    }

    private void updateInventoryViewAfterRemoval(String itemType, int amount) {
        renderFullInventory();
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
        updateInventorySelection();
    }
    private void updateInventorySelection() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            StackPane slot = (StackPane) inventoryBox.getChildren().get(i);
            if (i == selectedInventoryIndex) {
                slot.setStyle("-fx-border-color: gold; -fx-border-width: 3;");
            } else {
                slot.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
            }
        }
    }

    private String getSelectedInventoryItemType() {
        StackPane selectedSlot = (StackPane) inventoryBox.getChildren().get(selectedInventoryIndex);
        Object userData = selectedSlot.getUserData();
        if (userData != null && !userData.equals("empty")) {
            return userData.toString();
        }
        return null;
    }

    private void handleInput(double deltaTime) {
        if (activeKeys.contains(KeyCode.A)) {
            player.moveLeft(deltaTime);
        } else if (activeKeys.contains(KeyCode.D)) {
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
        String selectedType = getSelectedInventoryItemType();
        player.setHasBoots("boots".equals(selectedType));

        if (player.attackJustStarted()) {
            double px = player.getX() + player.getWidth() / 2;
            double py = player.getY() + player.getHeight() / 2;

            for (Enemy enemy : enemies) {
                double ex = enemy.getX() + enemy.getWidth() / 2;
                double ey = enemy.getY() + enemy.getHeight() / 2;
                double dist = Math.hypot(px - ex, py - ey);
                if (dist <= player.getAttackRadius() && !enemy.isDead()) {
                    enemy.die();
                }
            }
        }
        player.resetAttackJustStarted();

        player.update(deltaTime, platforms, worldWidth, gameCanvas.getHeight(), System.nanoTime());

        long now = System.nanoTime();
        Iterator<Enemy> enemyIterator = enemies.iterator();
        boolean playerKilled = false;

        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update(deltaTime, platforms, player, now);

            if (enemy.tryAttackPlayer(player)) {
                hideMenus();
                restartGame();
                return;
            }

            if (enemy.shouldBeRemoved()) {
                enemyIterator.remove();
            }
        }

        double canvasCenter = gameCanvas.getWidth() / 2;
        cameraX = player.getX() - canvasCenter + player.getWidth() / 2;
        cameraX = Math.max(0, Math.min(cameraX, worldWidth - gameCanvas.getWidth()));

        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                inventoryItems.put(item.getType(), inventoryItems.getOrDefault(item.getType(), 0) + 1);
                collectedItemPositions.add(new Point2D(item.getX(), item.getY()));
                renderFullInventory();
                iterator.remove();
            }
        }

        if (castle != null && castle.intersects(player)) {
            gameWon = true;
            showVictoryMenu();
            return;
        }

        if (player.getY() > gameCanvas.getHeight() + 100) {
            hideMenus();
            restartGame();
            return;
        }

        if (showDialog) {
            boolean stillNearNPC = false;
            for (NPC npc : npcs) {
                if (npc.isNear(player.getX(), player.getY())) {
                    stillNearNPC = true;
                    break;
                }
            }
            for (Bush bush : bushes) {
                if (bush.hasBerry() && bush.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    bush.pickBerry();
                    inventoryItems.put("berry", inventoryItems.getOrDefault("berry", 0) + 1);
                    collectedBushPositions.add(new Point2D(bush.getX(), bush.getY()));
                    renderFullInventory();
                    break;
                }
            }
            if (!stillNearNPC) {
                showDialog = false;
            }
        }
    }

    private void showVictoryMenu() {
        victoryMenuBox.setVisible(true);
        victoryMenuBox.toFront();
        paused = true;
    }

    private void showPauseMenu() {
        pauseMenuBox.setVisible(true);
        pauseMenuBox.toFront();
        paused = true;
    }

    private void hideMenus() {
        victoryMenuBox.setVisible(false);
        pauseMenuBox.setVisible(false);
        paused = false;
    }

    private void restartGame() {
        hideMenus();
        gameWon = false;
        infoLabel.setText("Info: Press A/D to move, SPACE to jump, C to Craft, E to interact");
        inventoryItems.clear();
        collectedItemPositions.clear();
        collectedBushPositions.clear();
        initializeInventory();
        loadLevelFromJson(currentLevel);
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
            npc.update(System.nanoTime());
            npc.render(gc, cameraX);
        }
        for (Item item : items) {
            item.render(gc, cameraX);
        }
        for (Bush bush : bushes) {
            bush.render(gc, cameraX);
        }
        for (Enemy enemy : enemies) {
            enemy.render(gc, cameraX);
        }
        if (castle != null) {
            castle.render(gc, cameraX);
        }
    }

    private void loadLevelFromJson(String filename) {
        InputStream is = getClass().getResourceAsStream("/levels/" + filename);
        if (is == null) {
            GAME_LOGGER.severe("Level file not found: " + filename);
            return;
        }
        JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

        platforms.clear();
        npcs.clear();
        items.clear();
        enemies.clear();
        bushes.clear();
        castle = null;

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
            npcs.add(new NPC(x, y));
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

        JsonArray enemiesarray = json.getAsJsonArray("enemies");
        for (JsonElement eElem : enemiesarray) {
            JsonObject e = eElem.getAsJsonObject();
            double x = e.get("x").getAsDouble();
            double y = e.get("y").getAsDouble();
            double width = e.get("width").getAsDouble();
            double height = e.get("height").getAsDouble();
            enemies.add(new Enemy(x, y, width, height));
        }

        if (json.has("castle")) {
            JsonObject castleObj = json.getAsJsonObject("castle");
            double cx = castleObj.get("x").getAsDouble();
            double cy = castleObj.get("y").getAsDouble();
            castle = new Castle(cx, cy);
        }

        GAME_LOGGER.info("Loaded level: " + filename);
    }

    private List<String> getAllLevelFiles() {
        try {
            URI uri = getClass().getResource("/levels/").toURI();
            Path levelsPath;
            if (uri.getScheme().equals("jar")) {
                FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                levelsPath = fs.getPath("/levels/");
            } else {
                levelsPath = Paths.get(uri);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(levelsPath, "*.json")) {
                List<String> files = StreamSupport.stream(stream.spliterator(), false)
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
                GAME_LOGGER.info("Levels found: " + files);
                return files;
            }
        } catch (IOException | URISyntaxException e) {
            GAME_LOGGER.log(Level.SEVERE, "Couldn't get level files", e);
            return List.of("level1.json");
        }
    }
}