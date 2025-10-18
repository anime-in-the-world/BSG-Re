package com.birdsenger.controllers;

import com.birdsenger.BirdSengerApp;
import com.birdsenger.models.User;
import com.birdsenger.services.AuthService;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Hyperlink createAccountLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;

    private AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        User user = authService.login(username, password);

        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
            loadMainView();
        } else {
            showError("Incorrect password");
        }
    }

    @FXML
    private void handleCreateAccount() {
        switchScene("/fxml/signup.fxml", "BirdSenger - Create Account");
    }

    @FXML
    private void handleForgotPassword() {
        switchScene("/fxml/forgot-password.fxml", "BirdSenger - Reset Password");
    }

    private void loadMainView() {
        switchScene("/fxml/main.fxml", "BirdSenger");
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
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}