package com.birdsenger.controllers;

import com.birdsenger.utils.ProfilePictureUtil;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class MainController {

    @FXML private BorderPane mainContainer;
    @FXML private Label nameLabel;
    @FXML private Label usernameLabel;
    @FXML private StackPane contentArea;
    @FXML private Circle userAvatar;

    // Menu buttons
    @FXML private Button dashboardBtn;
    @FXML private Button messagesBtn;
    @FXML private Button bankBtn;
    @FXML private Button transactionsBtn;
    @FXML private Button restaurantsBtn;
    @FXML private Button utilitiesBtn;
    @FXML private Button settingsBtn;

    private String currentView = "dashboard";

    @FXML
    public void initialize() {
        // Set user info
        if (SessionManager.getInstance().isLoggedIn()) {
            nameLabel.setText(SessionManager.getInstance().getCurrentUser().getFullName());
            usernameLabel.setText(SessionManager.getInstance().getCurrentUser().getUsername());

            // Load profile picture
            loadProfilePicture();
        }

        // Load dashboard by default
        loadDashboard();
    }

    private void loadProfilePicture() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        String profilePic = ProfilePictureUtil.getProfilePicture(userId);

        if (profilePic != null && !profilePic.isEmpty()) {
            // Create ImageView with circular clip
            ImageView profileImageView = ProfilePictureUtil.createCircularImageView(profilePic, 20);

            // Replace the Circle with ImageView in the parent
            StackPane parent = (StackPane) userAvatar.getParent();
            int index = parent.getChildren().indexOf(userAvatar);
            parent.getChildren().set(index, profileImageView);
        }
        // If no profile picture, keep the default Circle
    }

    @FXML
    private void handleDashboard() {
        if (!currentView.equals("dashboard")) {
            loadDashboard();
            currentView = "dashboard";
        }
    }

    @FXML
    private void handleMessages() {
        if (!currentView.equals("messages")) {
            loadView("/fxml/messages.fxml");
            currentView = "messages";
        }
    }

    @FXML
    private void handleBank() {
        if (!currentView.equals("bank")) {
            loadView("/fxml/bank.fxml");
            currentView = "bank";
        }
    }

    @FXML
    private void handleTransactions() {
        if (!currentView.equals("transactions")) {
            loadView("/fxml/transactions.fxml");
            currentView = "transactions";
        }
    }

    @FXML
    private void handleRestaurants() {
        if (!currentView.equals("restaurants")) {
            loadView("/fxml/restaurants.fxml");
            currentView = "restaurants";
        }
    }

    @FXML
    private void handleUtilities() {
        if (!currentView.equals("utilities")) {
            loadView("/fxml/utilities.fxml");
            currentView = "utilities";
        }
    }

    @FXML
    private void handleSettings() {
        if (!currentView.equals("settings")) {
            loadView("/fxml/settings.fxml");
            currentView = "settings";
        }
    }

    @FXML
    private void handleLogout() {
        // Disconnect socket
        com.birdsenger.utils.SocketClient.getInstance().disconnect();

        SessionManager.getInstance().logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(root, com.birdsenger.BirdSengerApp.WINDOW_WIDTH,
                    com.birdsenger.BirdSengerApp.WINDOW_HEIGHT));
            stage.setTitle("BirdSenger - Sign In");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDashboard() {
        loadView("/fxml/dashboard.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("❌ Failed to load view: " + fxmlPath);
            e.printStackTrace();

            // Show error in content area
            Label errorLabel = new Label("Failed to load view: " + fxmlPath);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }
}