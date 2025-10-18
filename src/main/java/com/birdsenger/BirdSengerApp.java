package com.birdsenger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BirdSengerApp extends Application {

    public static final double WINDOW_WIDTH = 1280;
    public static final double WINDOW_HEIGHT = 820;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            primaryStage.setTitle("BirdSenger - Sign In");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(WINDOW_WIDTH);
            primaryStage.setMinHeight(WINDOW_HEIGHT);
            primaryStage.setMaxWidth(WINDOW_WIDTH);
            primaryStage.setMaxHeight(WINDOW_HEIGHT);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ Failed to load login screen");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}