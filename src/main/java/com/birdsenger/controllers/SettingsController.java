package com.birdsenger.controllers;

import com.birdsenger.utils.DatabaseManager;
import com.birdsenger.utils.ProfilePictureUtil;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SettingsController {

    @FXML private ImageView profileImageView;
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private Label messageLabel;

    private String currentProfilePicturePath;

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
        loadUserProfile();

        // Make profile picture circular
        Circle clip = new Circle(60, 60, 60);
        profileImageView.setClip(clip);
    }

    private void loadUserProfile() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        String sql = "SELECT first_name, last_name, username, email, profile_picture FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                nameField.setText(fullName);
                usernameField.setText(rs.getString("username"));
                emailField.setText(rs.getString("email"));

                currentProfilePicturePath = rs.getString("profile_picture");
                if (currentProfilePicturePath != null && !currentProfilePicturePath.isEmpty()) {
                    loadProfileImage(currentProfilePicturePath);
                } else {
                    // Load default image
                    Image defaultImg = ProfilePictureUtil.getDefaultImage();
                    if (defaultImg != null) {
                        profileImageView.setImage(defaultImg);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Read file as base64 and store in database
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);

                // Store base64 in database
                updateProfilePicture(base64Image);

                // Load new image
                Image image = ProfilePictureUtil.loadImage(base64Image);
                profileImageView.setImage(image);

                showMessage("Profile picture updated successfully!", "#10B981");

            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Failed to upload profile picture", "#EF4444");
            }
        }
    }

    private void updateProfilePicture(String base64Image) {
        String sql = "UPDATE users SET profile_picture = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, base64Image);
            stmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
            stmt.executeUpdate();

            currentProfilePicturePath = base64Image;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProfileImage(String base64OrPath) {
        Image image = ProfilePictureUtil.loadImage(base64OrPath);
        if (image != null) {
            profileImageView.setImage(image);
        }
    }

    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}