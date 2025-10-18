package com.birdsenger;

import com.birdsenger.utils.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class BirdSengerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        Label titleLabel = new Label("🐦 BirdSenger");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label statusLabel = new Label("Testing connection...");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        Button testButton = new Button("Refresh Connection Test");
        testButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");

        Label usersLabel = new Label("Loading users...");
        usersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        testButton.setOnAction(e -> {
            testConnection(statusLabel);
            loadUsers(usersLabel);
        });

        root.getChildren().addAll(titleLabel, statusLabel, testButton, usersLabel);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("BirdSenger - Database Test");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Auto-test on startup in background thread
        new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for UI to render
                Platform.runLater(() -> {
                    testConnection(statusLabel);
                    loadUsers(usersLabel);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void testConnection(Label statusLabel) {
        try {
            boolean connected = DatabaseManager.getInstance().testConnection();
            if (connected) {
                statusLabel.setText("✅ Connected to PostgreSQL on VPS (13.229.229.125)");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #90EE90;");
            } else {
                statusLabel.setText("❌ Connection Failed!");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF6B6B;");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error: " + e.getMessage());
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF6B6B;");
            e.printStackTrace();
        }
    }

    private void loadUsers(Label usersLabel) {
        new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, first_name, last_name, balance FROM users LIMIT 5")) {

                StringBuilder users = new StringBuilder("📋 Test Users from Database:\n\n");
                int count = 0;
                while (rs.next()) {
                    count++;
                    users.append(String.format("  %d. %s %s (@%s) - Balance: $%.2f\n",
                            count,
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("username"),
                            rs.getDouble("balance")
                    ));
                }

                if (count == 0) {
                    users.append("  No users found in database!");
                }

                String finalText = users.toString();
                Platform.runLater(() -> {
                    usersLabel.setText(finalText);
                    usersLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-font-family: monospace;");
                });

                System.out.println("✅ Successfully loaded " + count + " users from database");

            } catch (Exception e) {
                String error = "❌ Error loading users: " + e.getMessage();
                Platform.runLater(() -> {
                    usersLabel.setText(error);
                    usersLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF6B6B;");
                });
                System.err.println(error);
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}