package com.birdsenger.controllers;

import com.birdsenger.utils.DatabaseManager;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class TransactionsController {

    @FXML private VBox transactionsContainer;
    @FXML private Button allButton;
    @FXML private Button sentButton;
    @FXML private Button receivedButton;

    private String currentFilter = "all";

    @FXML
    public void initialize() {
        loadTransactions("all");
    }

    @FXML
    private void handleAll() {
        currentFilter = "all";
        updateButtonStyles();
        loadTransactions("all");
    }

    @FXML
    private void handleSent() {
        currentFilter = "sent";
        updateButtonStyles();
        loadTransactions("sent");
    }

    @FXML
    private void handleReceived() {
        currentFilter = "received";
        updateButtonStyles();
        loadTransactions("received");
    }

    private void updateButtonStyles() {
        allButton.getStyleClass().remove("filter-button-active");
        sentButton.getStyleClass().remove("filter-button-active");
        receivedButton.getStyleClass().remove("filter-button-active");

        allButton.getStyleClass().add("filter-button");
        sentButton.getStyleClass().add("filter-button");
        receivedButton.getStyleClass().add("filter-button");

        switch (currentFilter) {
            case "all":
                allButton.getStyleClass().remove("filter-button");
                allButton.getStyleClass().add("filter-button-active");
                break;
            case "sent":
                sentButton.getStyleClass().remove("filter-button");
                sentButton.getStyleClass().add("filter-button-active");
                break;
            case "received":
                receivedButton.getStyleClass().remove("filter-button");
                receivedButton.getStyleClass().add("filter-button-active");
                break;
        }
    }

    private void loadTransactions(String filter) {
        int userId = SessionManager.getInstance().getCurrentUserId();

        StringBuilder sql = new StringBuilder(
                "SELECT t.*, " +
                        "sender.first_name as sender_first, sender.last_name as sender_last, " +
                        "receiver.first_name as receiver_first, receiver.last_name as receiver_last " +
                        "FROM transactions t " +
                        "LEFT JOIN users sender ON t.sender_id = sender.id " +
                        "LEFT JOIN users receiver ON t.receiver_id = receiver.id " +
                        "WHERE (t.sender_id = ? OR t.receiver_id = ?) "
        );

        if (filter.equals("sent")) {
            sql.append("AND t.sender_id = ? ");
        } else if (filter.equals("received")) {
            sql.append("AND t.receiver_id = ? ");
        }

        sql.append("ORDER BY t.timestamp DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            if (!filter.equals("all")) {
                stmt.setInt(3, userId);
            }

            ResultSet rs = stmt.executeQuery();

            transactionsContainer.getChildren().clear();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");

            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                int receiverId = rs.getInt("receiver_id");
                double amount = rs.getDouble("amount");
                String timestamp = dateFormat.format(rs.getTimestamp("timestamp"));
                String status = rs.getString("status");

                String senderName = rs.getString("sender_first") + " " + rs.getString("sender_last");
                String receiverName = rs.getString("receiver_first") + " " + rs.getString("receiver_last");

                String entity;
                String type;
                if (senderId == userId) {
                    entity = receiverName;
                    type = "Sent";
                } else {
                    entity = senderName;
                    type = "Received";
                }

                HBox transactionRow = createTransactionRow(timestamp, entity, amount, type, status);
                transactionsContainer.getChildren().add(transactionRow);
            }

            if (transactionsContainer.getChildren().isEmpty()) {
                Label noData = new Label("No transactions found");
                noData.setStyle("-fx-font-size: 16px; -fx-text-fill: #9CA3AF; -fx-padding: 50 0;");
                transactionsContainer.getChildren().add(noData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createTransactionRow(String date, String entity, double amount,
                                      String status, String txStatus) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("transaction-row");
        row.setPadding(new Insets(20));

        // Date
        Label dateLabel = new Label(date);
        dateLabel.setPrefWidth(120);
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        // Entity
        Label entityLabel = new Label(entity);
        entityLabel.setPrefWidth(200);
        entityLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1F2937;");

        // Amount
        Label amountLabel = new Label(String.format("$%.2f", amount));
        amountLabel.setPrefWidth(120);
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " +
                (status.equals("Sent") ? "#EF4444" : "#10B981") + ";");

        // Status
        Label statusLabel = new Label(status);
        statusLabel.setPrefWidth(100);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " +
                (status.equals("Sent") ? "#EF4444" : "#10B981") + ";");

        // View Details Button
        Button viewButton = new Button("View Details");
        viewButton.getStyleClass().add("view-details-button");

        row.getChildren().addAll(dateLabel, entityLabel, amountLabel, statusLabel, viewButton);

        return row;
    }
}