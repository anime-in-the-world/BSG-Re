package com.birdsenger.controllers;

import com.birdsenger.utils.DatabaseManager;
import com.birdsenger.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class BankController {

    @FXML private Label totalBalanceLabel;
    @FXML private VBox accountsContainer;

    @FXML
    public void initialize() {
        loadBankData();
    }

    private void loadBankData() {
        int userId = SessionManager.getInstance().getCurrentUserId();

        // Load total balance
        loadTotalBalance(userId);

        // Load bank accounts
        loadBankAccounts(userId);
    }

    private void loadTotalBalance(int userId) {
        String sql = "SELECT balance FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                totalBalanceLabel.setText(String.format("$%,.2f", balance));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBankAccounts(int userId) {
        String sql = "SELECT * FROM bank_accounts WHERE user_id = ? ORDER BY linked_date DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            accountsContainer.getChildren().clear();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM, yyyy");

            while (rs.next()) {
                String bankName = rs.getString("bank_name");
                String accountNumber = rs.getString("account_number");
                String accountType = rs.getString("account_type");
                double balance = rs.getDouble("balance");
                String linkedDate = dateFormat.format(rs.getTimestamp("linked_date"));
                String status = rs.getString("status");

                VBox accountCard = createAccountCard(bankName, accountNumber, accountType,
                        balance, linkedDate, status);
                accountsContainer.getChildren().add(accountCard);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createAccountCard(String bankName, String accountNumber, String accountType,
                                   double balance, String linkedDate, String status) {
        VBox card = new VBox(15);
        card.getStyleClass().add("bank-account-card");

        // Bank name and balance
        Label nameLabel = new Label(bankName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label balanceLabel = new Label(String.format("$%,.2f", balance));
        balanceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #EC6D87;");

        // Account details
        Label accountNumberLabel = new Label("Account Number: " + maskAccountNumber(accountNumber));
        accountNumberLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        Label accountTypeLabel = new Label("Account Type: " + accountType);
        accountTypeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        Label linkedDateLabel = new Label("Linked Date: " + linkedDate);
        linkedDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        Label statusLabel = new Label("Status: " + status.toUpperCase());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " +
                (status.equals("active") ? "#10B981" : "#EF4444") + ";");

        card.getChildren().addAll(nameLabel, balanceLabel, accountNumberLabel,
                accountTypeLabel, linkedDateLabel, statusLabel);

        return card;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) return accountNumber;
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }

    @FXML
    private void handleRefresh() {
        loadBankData();
    }
}