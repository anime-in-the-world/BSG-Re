package com.birdsenger.controllers;

import com.birdsenger.utils.DatabaseManager;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label unreadMessagesLabel;
    @FXML private Label balanceLabel;
    @FXML private Label lastTransactionLabel;

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        String firstName = SessionManager.getInstance().getCurrentUser().getFirstName();
        welcomeLabel.setText("Welcome back, " + firstName + "!");

        // Set current date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");
        dateLabel.setText("Today: " + now.format(formatter));

        // Load unread messages count
        loadUnreadMessages();

        // Load balance
        loadBalance();

        // Load last transaction
        loadLastTransaction();
    }

    private void loadUnreadMessages() {
        String sql = "SELECT COUNT(DISTINCT m.conversation_id) as unread_count " +
                "FROM messages m " +
                "JOIN conversation_members cm ON m.conversation_id = cm.conversation_id " +
                "WHERE cm.user_id = ? AND m.sender_id != ? AND m.is_read = false";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int userId = SessionManager.getInstance().getCurrentUserId();
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("unread_count");
                unreadMessagesLabel.setText(count + " unread messages");
            }

        } catch (Exception e) {
            e.printStackTrace();
            unreadMessagesLabel.setText("0 unread messages");
        }
    }

    private void loadBalance() {
        String sql = "SELECT balance FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceLabel.setText(String.format("Balance: $%,.2f", balance));
            }

        } catch (Exception e) {
            e.printStackTrace();
            balanceLabel.setText("Balance: $0.00");
        }
    }

    private void loadLastTransaction() {
        String sql = "SELECT t.*, u.first_name, u.last_name " +
                "FROM transactions t " +
                "LEFT JOIN users u ON (CASE WHEN t.sender_id = ? THEN t.receiver_id ELSE t.sender_id END) = u.id " +
                "WHERE t.sender_id = ? OR t.receiver_id = ? " +
                "ORDER BY t.timestamp DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int userId = SessionManager.getInstance().getCurrentUserId();
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int senderId = rs.getInt("sender_id");
                double amount = rs.getDouble("amount");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");

                String text;
                if (senderId == userId) {
                    text = String.format("You sent $%.2f to %s %s", amount, firstName, lastName);
                } else {
                    text = String.format("%s %s sent $%.2f", firstName, lastName, amount);
                }
                lastTransactionLabel.setText(text);
            } else {
                lastTransactionLabel.setText("No transactions yet");
            }

        } catch (Exception e) {
            e.printStackTrace();
            lastTransactionLabel.setText("No transactions");
        }
    }
}