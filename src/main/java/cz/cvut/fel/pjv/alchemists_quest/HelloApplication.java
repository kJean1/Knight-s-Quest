package cz.cvut.fel.pjv.alchemists_quest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.IOException;
import java.net.URL;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        String fxmlFile = "hello-view.fxml";
        URL fxmlUrl = getClass().getResource(fxmlFile);
        if (fxmlUrl == null) {
            System.err.println("Cannot find FXML file: " + fxmlFile);
            return;
        }
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        stage.setTitle("Alchemist`s Quest");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        Platform.runLater(() -> {
            scene.lookup("#gameCanvas").requestFocus();
        });

        root.requestFocus();
    }

    public static void main(String[] args) { 
        launch(args);
    }
}