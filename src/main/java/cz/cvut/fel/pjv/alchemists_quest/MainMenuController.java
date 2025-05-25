package cz.cvut.fel.pjv.alchemists_quest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.logging.*;

public class MainMenuController {
    @FXML private ImageView logoView;
    @FXML private Button playButton;
    @FXML private ComboBox<String> levelComboBox;
    @FXML private CheckBox loggingCheckBox;

    public static final Logger GAME_LOGGER = Logger.getLogger("AlchemistsQuestLogger");
    public static boolean loggingEnabled = false;

    @FXML
    public void initialize() {
        // Логотип (замени на свой путь при необходимости)
        try {
            Image logoImg = new Image(getClass().getResource("/logo.png").toExternalForm());
            logoView.setImage(logoImg);
        } catch (Exception ex) {
            // Если картинки нет — не падаем
        }

        // Уровни
        levelComboBox.getItems().addAll("Level 1", "Level 2", "Level 3");
        levelComboBox.setValue("Level 1");

        // Логгер
        setupLogger();
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

        playButton.setOnAction(this::onPlayClicked);
    }

    private void onPlayClicked(ActionEvent e) {
        String selectedLevel = levelComboBox.getValue();
        if (loggingEnabled) GAME_LOGGER.info("Starting game at " + selectedLevel);

        // Загрузка игрового экрана (hello-view.fxml) в тот же Scene
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent gameRoot = loader.load();

            // Передать параметры уровня и логирования можно через статические поля, синглтон или в контроллер hello-view
            playButton.getScene().setRoot(gameRoot);
        } catch (IOException ex) {
            ex.printStackTrace();
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
}