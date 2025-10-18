package com.birdsenger.utils;

import com.birdsenger.controllers.MessagesController;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private MessagesController messagesController;

    // Socket server on VPS
    private static final String SOCKET_SERVER = "http://13.229.229.125:9092";

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect(int userId) {
        try {
            socket = IO.socket(SOCKET_SERVER);

            socket.on(Socket.EVENT_CONNECT, args -> {
                System.out.println("✅ Connected to Socket.IO server");

                // Notify server of user connection
                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                socket.emit("user_connected", new JSONObject(data));
            });

            socket.on("new_message", args -> {
                if (messagesController != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    javafx.application.Platform.runLater(() -> {
                        try {
                            messagesController.handleNewMessage(data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

            socket.on("money_received", args -> {
                if (messagesController != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    javafx.application.Platform.runLater(() -> {
                        try {
                            messagesController.handleMoneyReceived(data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

            socket.on("friend_status_changed", args -> {
                if (messagesController != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    javafx.application.Platform.runLater(() ->
                            messagesController.handleFriendStatusChanged(data));
                }
            });

            socket.on("new_notification", args -> {
                if (messagesController != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    javafx.application.Platform.runLater(() ->
                            messagesController.handleNotification(data));
                }
            });

            socket.on("friend_request_response", args -> {
                if (messagesController != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    javafx.application.Platform.runLater(() -> {
                        try {
                            messagesController.handleFriendRequestResponse(data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            System.err.println("❌ Failed to connect to Socket.IO server");
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            System.out.println("👋 Disconnected from Socket.IO server");
        }
    }

    public void sendMessage(int conversationId, int senderId, String content, String messageType) {
        if (socket != null && socket.connected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversationId);
            data.put("senderId", senderId);
            data.put("content", content);
            data.put("messageType", messageType);

            socket.emit("send_message", new JSONObject(data));
        }
    }

    public void sendMoney(int senderId, int receiverId, double amount, int conversationId) {
        if (socket != null && socket.connected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("senderId", senderId);
            data.put("receiverId", receiverId);
            data.put("amount", amount);
            data.put("conversationId", conversationId);

            socket.emit("send_money", new JSONObject(data));
        }
    }

    public void sendFriendRequest(int senderId, String receiverUsername) {
        if (socket != null && socket.connected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("senderId", senderId);
            data.put("receiverUsername", receiverUsername);

            socket.emit("send_friend_request", new JSONObject(data));
        }
    }

    public void respondToFriendRequest(int requestId, boolean accept) {
        if (socket != null && socket.connected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("requestId", requestId);
            data.put("accept", accept);

            socket.emit("respond_friend_request", new JSONObject(data));
        }
    }

    public void setMessagesController(MessagesController controller) {
        this.messagesController = controller;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}