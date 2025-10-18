package com.birdsenger.controllers;

import com.birdsenger.BirdSengerApp;
import com.birdsenger.services.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> securityQuestionCombo;
    @FXML private TextField securityAnswerField;
    @FXML private Label messageLabel;
    @FXML private Hyperlink loginLink;
    @FXML private Button createAccountButton;

    private AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        securityQuestionCombo.getItems().addAll(
                "Where was your first honeymoon?",
                "What is your favorite color?",
                "What is your pet's name?",
                "What city were you born in?",
                "What is your mother's maiden name?"
        );
        securityQuestionCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleCreateAccount() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String securityQuestion = securityQuestionCombo.getValue();
        String securityAnswer = securityAnswerField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                securityAnswer.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords don't match");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!email.contains("@")) {
            showError("Invalid email address");
            return;
        }

        boolean success = authService.signup(firstName, lastName, email, username,
                password, securityQuestion, securityAnswer);

        if (success) {
            showSuccess("Account Created Successfully");
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::handleLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Username or email already exists");
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

            Stage stage = (Stage) firstNameField.getScene().getWindow();
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
        messageLabel.setStyle("-fx-text-fill: #EF4444;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #10B981;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}