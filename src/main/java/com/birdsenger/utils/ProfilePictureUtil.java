package com.birdsenger.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfilePictureUtil {

    /**
     * Load profile picture from database for a user
     */
    public static String getProfilePicture(int userId) {
        String sql = "SELECT profile_picture FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("profile_picture");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Create an ImageView with circular clip from base64 or path
     */
    public static ImageView createCircularImageView(String base64OrPath, double radius) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(radius * 2);
        imageView.setFitHeight(radius * 2);
        imageView.setPreserveRatio(true);

        // Make it circular
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);

        // Load image
        Image image = loadImage(base64OrPath);
        if (image != null) {
            imageView.setImage(image);
        } else {
            // Use default image
            imageView.setImage(getDefaultImage());
        }

        return imageView;
    }

    /**
     * Load image from base64 string or file path
     */
    public static Image loadImage(String base64OrPath) {
        if (base64OrPath == null || base64OrPath.isEmpty()) {
            return getDefaultImage();
        }

        try {
            // Check if it's base64 (longer than typical file paths)
            if (base64OrPath.length() > 500 || base64OrPath.startsWith("data:")) {
                String base64Data = base64OrPath;
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                return new Image(new ByteArrayInputStream(imageBytes));
            } else {
                // It's a file path
                File imageFile = new File(base64OrPath);
                if (imageFile.exists()) {
                    return new Image(new FileInputStream(imageFile));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load profile image: " + e.getMessage());
        }

        return getDefaultImage();
    }

    /**
     * Get default avatar image
     */
    public static Image getDefaultImage() {
        try {
            return new Image(ProfilePictureUtil.class.getResourceAsStream("/images/default-avatar.png"));
        } catch (Exception e) {
            // Return null if default image not found - will use colored circle instead
            return null;
        }
    }
}