package com.birdsenger.server;

import com.corundumstudio.socketio.*;
import com.google.gson.Gson;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    private SocketIOServer server;
    private Map<Integer, UUID> userSessions = new ConcurrentHashMap<>();

    public SocketServer() {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(9092);
        server = new SocketIOServer(config);
        setupEvents();
    }

    private void setupEvents() {
        // User connection
        server.addEventListener("user_connected", Map.class, (client, data, ack) -> {
            int userId = ((Number) data.get("userId")).intValue();
            userSessions.put(userId, client.getSessionId());
            updateOnline(userId, true);
            System.out.println("✅ User " + userId + " connected");
        });

        // User disconnection
        server.addDisconnectListener(client -> {
            for (Map.Entry<Integer, UUID> e : userSessions.entrySet()) {
                if (e.getValue().equals(client.getSessionId())) {
                    updateOnline(e.getKey(), false);
                    userSessions.remove(e.getKey());
                    System.out.println("👋 User " + e.getKey() + " disconnected");
                    break;
                }
            }
        });

        // Send message
        server.addEventListener("send_message", Map.class, (client, data, ack) -> {
            int convId = ((Number) data.get("conversationId")).intValue();
            int senderId = ((Number) data.get("senderId")).intValue();
            String content = (String) data.get("content");

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://13.229.229.125:5432/birdsenger", "birduser", "BirdSecure2024!")) {

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO messages (conversation_id, sender_id, content, message_type) VALUES (?, ?, ?, 'text') RETURNING id");
                stmt.setInt(1, convId);
                stmt.setInt(2, senderId);
                stmt.setString(3, content);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("conversationId", convId);
                    msg.put("senderId", senderId);
                    msg.put("content", content);

                    PreparedStatement mem = conn.prepareStatement(
                            "SELECT user_id FROM conversation_members WHERE conversation_id = ?");
                    mem.setInt(1, convId);
                    ResultSet mems = mem.executeQuery();

                    while (mems.next()) {
                        int userId = mems.getInt(1);
                        UUID sid = userSessions.get(userId);
                        if (sid != null) {
                            server.getClient(sid).sendEvent("new_message", msg);
                        }
                    }
                    System.out.println("💬 Message sent in conversation " + convId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Send money
        server.addEventListener("send_money", Map.class, (client, data, ack) -> {
            int senderId = ((Number) data.get("senderId")).intValue();
            int receiverId = ((Number) data.get("receiverId")).intValue();
            double amount = ((Number) data.get("amount")).doubleValue();
            int convId = ((Number) data.get("conversationId")).intValue();

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://13.229.229.125:5432/birdsenger", "birduser", "BirdSecure2024!")) {

                conn.setAutoCommit(false);

                PreparedStatement deduct = conn.prepareStatement(
                        "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?");
                deduct.setDouble(1, amount);
                deduct.setInt(2, senderId);
                deduct.setDouble(3, amount);

                if (deduct.executeUpdate() > 0) {
                    PreparedStatement add = conn.prepareStatement(
                            "UPDATE users SET balance = balance + ? WHERE id = ?");
                    add.setDouble(1, amount);
                    add.setInt(2, receiverId);
                    add.executeUpdate();

                    PreparedStatement txn = conn.prepareStatement(
                            "INSERT INTO transactions (sender_id, receiver_id, amount, conversation_id, status) VALUES (?, ?, ?, ?, 'completed')");
                    txn.setInt(1, senderId);
                    txn.setInt(2, receiverId);
                    txn.setDouble(3, amount);
                    txn.setInt(4, convId);
                    txn.executeUpdate();

                    // Insert payment message
                    PreparedStatement msgStmt = conn.prepareStatement(
                            "INSERT INTO messages (conversation_id, sender_id, content, message_type) VALUES (?, ?, ?, 'payment')");
                    msgStmt.setInt(1, convId);
                    msgStmt.setInt(2, senderId);
                    msgStmt.setString(3, "Sent $" + amount);
                    msgStmt.executeUpdate();

                    conn.commit();

                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("amount", amount);

                    client.sendEvent("money_sent", resp);
                    UUID rid = userSessions.get(receiverId);
                    if (rid != null) {
                        server.getClient(rid).sendEvent("money_received", resp);

                        // Notify to reload messages
                        Map<String, Object> msgNotif = new HashMap<>();
                        msgNotif.put("conversationId", convId);
                        server.getClient(rid).sendEvent("new_message", msgNotif);
                    }

                    System.out.println("💰 Money sent: $" + amount + " from " + senderId + " to " + receiverId);
                } else {
                    conn.rollback();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", false);
                    resp.put("message", "Insufficient balance");
                    client.sendEvent("money_sent", resp);
                    System.out.println("❌ Money transfer failed: insufficient balance");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Send friend request
        server.addEventListener("send_friend_request", Map.class, (client, data, ack) -> {
            int senderId = ((Number) data.get("senderId")).intValue();
            String receiverUsername = (String) data.get("receiverUsername");

            System.out.println("📨 Friend request from user " + senderId + " to " + receiverUsername);

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://13.229.229.125:5432/birdsenger", "birduser", "BirdSecure2024!")) {

                // Find receiver by username or email
                PreparedStatement findUser = conn.prepareStatement(
                        "SELECT id FROM users WHERE username = ? OR email = ?");
                findUser.setString(1, receiverUsername);
                findUser.setString(2, receiverUsername);
                ResultSet userRs = findUser.executeQuery();

                if (userRs.next()) {
                    int receiverId = userRs.getInt("id");

                    // Check if they're the same user
                    if (senderId == receiverId) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "You cannot send a friend request to yourself");
                        client.sendEvent("friend_request_response", response);
                        System.out.println("❌ Cannot send request to self");
                        return;
                    }

                    // Check if already friends
                    PreparedStatement checkFriends = conn.prepareStatement(
                            "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?");
                    checkFriends.setInt(1, senderId);
                    checkFriends.setInt(2, receiverId);
                    ResultSet friendRs = checkFriends.executeQuery();

                    if (friendRs.next() && friendRs.getInt(1) > 0) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "You are already friends with this user");
                        client.sendEvent("friend_request_response", response);
                        System.out.println("❌ Already friends");
                        return;
                    }

                    // Check if request already exists
                    PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'");
                    checkStmt.setInt(1, senderId);
                    checkStmt.setInt(2, receiverId);
                    ResultSet checkRs = checkStmt.executeQuery();

                    if (checkRs.next() && checkRs.getInt(1) == 0) {
                        // Insert friend request
                        PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, 'pending')");
                        insertStmt.setInt(1, senderId);
                        insertStmt.setInt(2, receiverId);
                        insertStmt.executeUpdate();

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Friend request sent successfully!");
                        client.sendEvent("friend_request_response", response);

                        // Notify receiver
                        UUID receiverSession = userSessions.get(receiverId);
                        if (receiverSession != null) {
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("type", "friend_request");
                            notification.put("senderId", senderId);
                            server.getClient(receiverSession).sendEvent("new_notification", notification);
                            System.out.println("🔔 Notified receiver " + receiverId);
                        }

                        System.out.println("✅ Friend request created: " + senderId + " -> " + receiverId);
                    } else {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Friend request already sent");
                        client.sendEvent("friend_request_response", response);
                        System.out.println("❌ Friend request already exists");
                    }
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "User not found");
                    client.sendEvent("friend_request_response", response);
                    System.out.println("❌ User not found: " + receiverUsername);
                }

            } catch (Exception e) {
                System.err.println("❌ Error sending friend request:");
                e.printStackTrace();
            }
        });

        // Respond to friend request
        server.addEventListener("respond_friend_request", Map.class, (client, data, ack) -> {
            int requestId = ((Number) data.get("requestId")).intValue();
            boolean accept = (boolean) data.get("accept");

            System.out.println("📬 Responding to friend request " + requestId + ": " + (accept ? "ACCEPT" : "REJECT"));

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://13.229.229.125:5432/birdsenger", "birduser", "BirdSecure2024!")) {

                if (accept) {
                    // Get sender and receiver IDs
                    PreparedStatement getIds = conn.prepareStatement(
                            "SELECT sender_id, receiver_id FROM friend_requests WHERE id = ?");
                    getIds.setInt(1, requestId);
                    ResultSet rs = getIds.executeQuery();

                    if (rs.next()) {
                        int senderId = rs.getInt("sender_id");
                        int receiverId = rs.getInt("receiver_id");

                        // Create friendships (both directions)
                        PreparedStatement addFriend = conn.prepareStatement(
                                "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?), (?, ?) ON CONFLICT DO NOTHING");
                        addFriend.setInt(1, senderId);
                        addFriend.setInt(2, receiverId);
                        addFriend.setInt(3, receiverId);
                        addFriend.setInt(4, senderId);
                        addFriend.executeUpdate();

                        // Create a 1-on-1 conversation between them
                        PreparedStatement createConv = conn.prepareStatement(
                                "INSERT INTO conversations (is_group, created_by) VALUES (false, ?) RETURNING id");
                        createConv.setInt(1, receiverId);
                        ResultSet convRs = createConv.executeQuery();

                        if (convRs.next()) {
                            int convId = convRs.getInt("id");

                            // Add both users to the conversation
                            PreparedStatement addMembers = conn.prepareStatement(
                                    "INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?), (?, ?)");
                            addMembers.setInt(1, convId);
                            addMembers.setInt(2, senderId);
                            addMembers.setInt(3, convId);
                            addMembers.setInt(4, receiverId);
                            addMembers.executeUpdate();

                            System.out.println("💬 Created conversation " + convId + " for users " + senderId + " and " + receiverId);
                        }

                        // Update request status
                        PreparedStatement updateReq = conn.prepareStatement(
                                "UPDATE friend_requests SET status = 'accepted' WHERE id = ?");
                        updateReq.setInt(1, requestId);
                        updateReq.executeUpdate();

                        // Notify both users to refresh their chat lists
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "friend_accepted");

                        UUID senderSession = userSessions.get(senderId);
                        UUID receiverSession = userSessions.get(receiverId);

                        if (senderSession != null) {
                            server.getClient(senderSession).sendEvent("friend_status_changed", notification);
                        }
                        if (receiverSession != null) {
                            server.getClient(receiverSession).sendEvent("friend_status_changed", notification);
                        }

                        System.out.println("✅ Friend request accepted: " + senderId + " <-> " + receiverId);
                    }
                } else {
                    // Reject - just update status
                    PreparedStatement updateReq = conn.prepareStatement(
                            "UPDATE friend_requests SET status = 'rejected' WHERE id = ?");
                    updateReq.setInt(1, requestId);
                    updateReq.executeUpdate();

                    System.out.println("❌ Friend request rejected: " + requestId);
                }

            } catch (Exception e) {
                System.err.println("❌ Error responding to friend request:");
                e.printStackTrace();
            }
        });
    }

    private void updateOnline(int userId, boolean online) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://13.229.229.125:5432/birdsenger", "birduser", "BirdSecure2024!")) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET is_online = ? WHERE id = ?");
            stmt.setBoolean(1, online);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        server.start();
        System.out.println("🚀 Socket server started on port 9092");
    }

    public static void main(String[] args) {
        new SocketServer().start();
    }
}