package com.birdsenger.controllers;

import com.birdsenger.BirdSengerApp;
import com.birdsenger.services.AuthService;
import com.birdsenger.utils.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ForgotPasswordController {

    @FXML private TextField usernameField;
    @FXML private Label securityQuestionLabel;
    @FXML private TextField answerField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label messageLabel;
    @FXML private Hyperlink loginLink;
    @FXML private Button confirmButton;

    private AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        securityQuestionLabel.setText("");

        usernameField.setOnAction(e -> loadSecurityQuestion());
        usernameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !usernameField.getText().trim().isEmpty()) {
                loadSecurityQuestion();
            }
        });
    }

    private void loadSecurityQuestion() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) return;

        String sql = "SELECT security_question FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String question = rs.getString("security_question");
                securityQuestionLabel.setText(question);
                securityQuestionLabel.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 14px;");
            } else {
                securityQuestionLabel.setText("User not found");
                securityQuestionLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConfirm() {
        String username = usernameField.getText().trim();
        String answer = answerField.getText().trim();
        String newPassword = newPasswordField.getText();

        if (username.isEmpty() || answer.isEmpty() || newPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        boolean success = authService.resetPassword(username, answer, newPassword);

        if (success) {
            showSuccess("Password Reset Successful");
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::handleLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Answer didn't match");
        }
    }

    @FXML
    private void handleLogin() {
        switchScene("/fxml/login.fxml", "BirdSenger - Sign In");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, BirdSengerApp.WINDOW_WIDTH, BirdSengerApp.WINDOW_HEIGHT);
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("❌ Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13px;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 13px;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}