package cz.cvut.fel.pjv.alchemists_quest;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    // private static final int WINDOW_WIDTH = 800;
    // private static final int WINDOW_HEIGHT = 600;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    activeKeys.add(event.getCode());

                    if (event.getCode() == KeyCode.C) {
                        craftingGrid.setVisible(!craftingGrid.isVisible());
                    }
                });

                newScene.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));
            }
        });

        gc = gameCanvas.getGraphicsContext2D();

        gameCanvas.setFocusTraversable(true);

        gameCanvas.setOnKeyPressed(event -> activeKeys.add(event.getCode()));
        gameCanvas.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));

        double canvasWidth = gameCanvas.getWidth();
        double canvasHeight = gameCanvas.getHeight();

        player = new Player(100, canvasHeight - PLAYER_HEIGHT - PLATFORM_HEIGHT - 50, PLAYER_WIDTH, PLAYER_HEIGHT);
        platforms.add(new Platform(50, canvasHeight - PLATFORM_HEIGHT - 20, PLATFORM_WIDTH, PLATFORM_HEIGHT));

        platforms.add(new Platform(200, 400, 150, 20));
        platforms.add(new Platform(500, 300, 200, 20));

        items.add(new Item(100, 100, "wood"));



        //game cycle
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                elapsedSeconds = Math.min(elapsedSeconds, 0.1); // Максимум 0.1 секунды на кадр

                handleInput(elapsedSeconds);

                update(elapsedSeconds);

                render();
            }
        };
        gameLoop.start();

        gameCanvas.requestFocus();
    }


    private void handleInput(double deltaTime) {
        // Movement
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) {
            player.moveLeft(deltaTime);
        } else if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) {
            player.moveRight(deltaTime);
        } else {
            player.stopHorizontalMovement();
        }
        // Jump
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.SPACE)) {
            if (player.isOnGround()) {
                player.jump();
            }
            // Убираем клавишу из набора, чтобы прыжок не повторялся при удержании
            // (простое решение, можно сделать и сложнее)
            activeKeys.remove(KeyCode.W);
            activeKeys.remove(KeyCode.SPACE);
        }
    }

    private void updateInventoryView() {
        inventoryBox.getChildren().clear(); // Очищаем старое содержимое

        for (String itemType : inventoryItems) {
            Label itemLabel = new Label();

            switch (itemType) {
                case "wood":
                    itemLabel.setText("🪵"); // Можно заменить на картинку потом
                    break;
                // Тут можно добавить другие предметы типа "stone", "metal" и т.д.
                default:
                    itemLabel.setText("❓"); // неизвестный предмет
                    break;
            }

            inventoryBox.getChildren().add(itemLabel);
        }
    }


    private void update(double deltaTime) {
        // Обновляем игрока, передавая все платформы
        player.update(deltaTime, platforms, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Проверяем пересечения с предметами
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                inventoryItems.add(item.getType()); // Добавляем в инвентарь
                updateInventoryView();              // Обновляем визуально HBox инвентаря
                iterator.remove();                  // Убираем предмет с поля
            }
        }
    }


    private void render() {
        // Рендер Canvas
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Рендер платформы
        gc.setFill(Color.DARKGREEN);
        for (Platform platform : platforms) {
            gc.fillRect(platform.getX(), platform.getY(), platform.getWidth(), platform.getHeight());
        }


        // Рендер игрока
        gc.setFill(Color.ORANGE);
        gc.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        // Рендер предметов
        for (Item item : items) {
            item.render(gc);
        }

    }
}