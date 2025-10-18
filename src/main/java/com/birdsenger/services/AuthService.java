package com.birdsenger.services;

import com.birdsenger.models.User;
import com.birdsenger.utils.DatabaseManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Random;

public class AuthService {

    public User login(String usernameOrEmail, String password) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, storedHash)) {
                    User user = mapResultSetToUser(rs);
                    System.out.println("✅ Login successful: " + user.getUsername());
                    return user;
                }
            }

            System.out.println("❌ Login failed: Invalid credentials");
            return null;

        } catch (SQLException e) {
            System.err.println("❌ Login error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean signup(String firstName, String lastName, String email,
                          String username, String password, String securityQuestion,
                          String securityAnswer) {

        // Check if username or email already exists
        if (userExists(username, email)) {
            System.out.println("❌ Signup failed: Username or email already exists");
            return false;
        }

        String sql = "INSERT INTO users (first_name, last_name, email, username, password_hash, " +
                "security_question, security_answer, balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            double initialBalance = 500 + new Random().nextDouble() * 4500; // Random between 500-5000

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, username);
            stmt.setString(5, hashedPassword);
            stmt.setString(6, securityQuestion);
            stmt.setString(7, securityAnswer.toLowerCase());
            stmt.setDouble(8, initialBalance);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Account created: " + username);

                // Create default bank account
                createDefaultBankAccount(username, initialBalance);
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ Signup error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean resetPassword(String username, String securityAnswer, String newPassword) {
        String checkSql = "SELECT id, security_answer FROM users WHERE username = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String storedAnswer = rs.getString("security_answer");

                if (storedAnswer.equalsIgnoreCase(securityAnswer.trim())) {
                    int userId = rs.getInt("id");
                    String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setInt(2, userId);

                    int rows = updateStmt.executeUpdate();

                    if (rows > 0) {
                        System.out.println("✅ Password reset successful for: " + username);
                        return true;
                    }
                }
            }

            System.out.println("❌ Password reset failed: Invalid username or security answer");
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Password reset error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean userExists(String username, String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void createDefaultBankAccount(String username, double balance) {
        String getUserIdSql = "SELECT id FROM users WHERE username = ?";
        String insertAccountSql = "INSERT INTO bank_accounts (user_id, account_number, account_type, " +
                "bank_name, balance) VALUES (?, ?, 'Savings', 'Eastern Bank Limited', ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement getUserStmt = conn.prepareStatement(getUserIdSql)) {

            getUserStmt.setString(1, username);
            ResultSet rs = getUserStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String accountNumber = generateAccountNumber();

                PreparedStatement insertStmt = conn.prepareStatement(insertAccountSql);
                insertStmt.setInt(1, userId);
                insertStmt.setString(2, accountNumber);
                insertStmt.setDouble(3, balance);

                insertStmt.executeUpdate();
                System.out.println("✅ Bank account created: " + accountNumber);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error creating bank account: " + e.getMessage());
        }
    }

    private String generateAccountNumber() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSecurityQuestion(rs.getString("security_question"));
        user.setSecurityAnswer(rs.getString("security_answer"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setBalance(rs.getDouble("balance"));
        user.setOnline(rs.getBoolean("is_online"));
        user.setLastSeen(rs.getTimestamp("last_seen"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}