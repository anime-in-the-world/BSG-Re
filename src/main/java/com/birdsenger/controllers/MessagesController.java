package com.birdsenger.controllers;

import com.birdsenger.utils.DatabaseManager;
import com.birdsenger.utils.SessionManager;
import com.birdsenger.utils.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.json.JSONObject;
import com.birdsenger.utils.ProfilePictureUtil;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessagesController {

    @FXML private VBox chatList;
    @FXML private StackPane contentPane;
    @FXML private Button onlineButton;
    @FXML private Button allButton;
    @FXML private Button addFriendsButton;
    @FXML private Button pendingButton;

    private int currentConversationId = -1;
    private VBox chatMessagesArea;
    private TextField messageInput;
    private ScrollPane chatScrollPane;

    @FXML
    public void initialize() {
        SocketClient.getInstance().setMessagesController(this);

        if (!SocketClient.getInstance().isConnected()) {
            SocketClient.getInstance().connect(SessionManager.getInstance().getCurrentUserId());
        }

        loadConversations();
        showDefaultView();
    }

    private void showDefaultView() {
        VBox defaultView = new VBox(15);
        defaultView.setAlignment(Pos.CENTER);
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 48px;");
        Label text = new Label("Select a chat to start messaging");
        text.setStyle("-fx-font-size: 16px; -fx-text-fill: #9CA3AF;");
        defaultView.getChildren().addAll(icon, text);
        contentPane.getChildren().setAll(defaultView);
    }

    @FXML
    private void handleOnline() {
        showOnlineFriends();
    }

    @FXML
    private void handleAll() {
        showAllFriends();
    }

    @FXML
    private void handleAddFriends() {
        showAddFriendsView();
    }

    @FXML
    private void handlePending() {
        showPendingRequests();
    }

    @FXML
    private void handleCreateGroup() {
        showCreateGroupView();
    }

    private void loadConversations() {
        String sql = "SELECT DISTINCT c.id, c.name, c.is_group, " +
                "(SELECT content FROM messages WHERE conversation_id = c.id ORDER BY timestamp DESC LIMIT 1) as last_msg, " +
                "(SELECT timestamp FROM messages WHERE conversation_id = c.id ORDER BY timestamp DESC LIMIT 1) as last_msg_time, " +
                "(SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = false) as unread_count " +
                "FROM conversations c " +
                "JOIN conversation_members cm ON c.id = cm.conversation_id " +
                "WHERE cm.user_id = ? " +
                "ORDER BY last_msg_time DESC NULLS LAST";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int currentUserId = SessionManager.getInstance().getCurrentUserId();
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, currentUserId);
            ResultSet rs = stmt.executeQuery();

            chatList.getChildren().clear();

            while (rs.next()) {
                int convId = rs.getInt("id");
                boolean isGroup = rs.getBoolean("is_group");
                String name = isGroup ? rs.getString("name") : getOtherUserName(convId);
                String lastMsg = rs.getString("last_msg");
                int unreadCount = rs.getInt("unread_count");
                boolean online = !isGroup && isUserOnline(getOtherUserId(convId));

                HBox chatItem = createChatItem(convId, name, lastMsg != null ? lastMsg : "", online, unreadCount);
                chatList.getChildren().add(chatItem);
            }

            if (chatList.getChildren().isEmpty()) {
                Label noChats = new Label("No conversations yet\nAdd friends to start chatting!");
                noChats.setStyle("-fx-text-fill: #9CA3AF; -fx-text-alignment: center;");
                noChats.setAlignment(Pos.CENTER);
                chatList.getChildren().add(noChats);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createChatItem(int convId, String name, String lastMsg, boolean online, int unreadCount) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
        item.getStyleClass().add("chat-item");

        // Store conversation ID in userData for quick lookup
        item.setUserData(convId);

        StackPane avatarPane = new StackPane();

        // Get profile picture for this conversation
        boolean isGroup = isConversationGroup(convId);
        if (!isGroup) {
            int otherUserId = getOtherUserId(convId);
            String profilePic = ProfilePictureUtil.getProfilePicture(otherUserId);

            if (profilePic != null && !profilePic.isEmpty()) {
                ImageView avatar = ProfilePictureUtil.createCircularImageView(profilePic, 25);
                avatarPane.getChildren().add(avatar);
            } else {
                Circle avatar = new Circle(25);
                avatar.setFill(Color.web("#EC6D87"));
                avatarPane.getChildren().add(avatar);
            }

            if (online) {
                Circle indicator = new Circle(8);
                indicator.setFill(Color.web("#10B981"));
                indicator.setStroke(Color.WHITE);
                indicator.setStrokeWidth(2);
                StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
                avatarPane.getChildren().add(indicator);
            }
        } else {
            Circle avatar = new Circle(25);
            avatar.setFill(Color.web("#EC6D87"));
            avatarPane.getChildren().add(avatar);
        }

        VBox textBox = new VBox(5);
        Label nameLabel = new Label(name);

        if (unreadCount > 0) {
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1F2937;");
        } else {
            nameLabel.setStyle("-fx-font-weight: normal; -fx-font-size: 14px; -fx-text-fill: #1F2937;");
        }

        Label msgLabel = new Label(lastMsg.length() > 40 ? lastMsg.substring(0, 40) + "..." : lastMsg);

        if (unreadCount > 0) {
            msgLabel.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            msgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        }

        textBox.getChildren().addAll(nameLabel, msgLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        if (unreadCount > 0) {
            Label unreadBadge = new Label(String.valueOf(unreadCount));
            unreadBadge.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; " +
                    "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            item.getChildren().addAll(avatarPane, textBox, unreadBadge);
        } else {
            item.getChildren().addAll(avatarPane, textBox);
        }

        item.setOnMouseClicked(e -> {
            markMessagesAsRead(convId);
            openChat(convId);
        });

        return item;
    }


    private void openChat(int conversationId) {
        currentConversationId = conversationId;
        contentPane.getChildren().clear();

        BorderPane chatView = new BorderPane();
        chatView.setStyle("-fx-background-color: white;");

        // Check if it's a group chat
        boolean isGroup = isConversationGroup(conversationId);

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        Label chatName = new Label(getConversationName(conversationId));
        chatName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isGroup) {
            // For group chats, show "Group Members" button
            Button membersBtn = new Button("👥 Group Members");
            membersBtn.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
            membersBtn.setOnAction(e -> showGroupMembers(conversationId));
            topBar.getChildren().addAll(chatName, spacer, membersBtn);
        } else {
            // For 1-on-1 chats, show "Send Money" button
            Button moneyBtn = new Button("💰 Send Money");
            moneyBtn.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
            moneyBtn.setOnAction(e -> showSendMoneyDialog());
            topBar.getChildren().addAll(chatName, spacer, moneyBtn);
        }

        // Messages area
        chatScrollPane = new ScrollPane();
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background: #F9FAFB; -fx-background-color: #F9FAFB;");
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        chatMessagesArea = new VBox(10);
        chatMessagesArea.setPadding(new Insets(20));
        chatMessagesArea.setStyle("-fx-background-color: #F9FAFB;");
        chatScrollPane.setContent(chatMessagesArea);

        loadMessages(conversationId);

        // Input area
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(15, 20, 15, 20));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-width: 1 0 0 0;");

        messageInput = new TextField();
        messageInput.setPromptText("Type your message...");
        messageInput.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 20; -fx-padding: 12 15;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 12 25; -fx-cursor: hand; -fx-font-weight: bold;");
        sendBtn.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        inputArea.getChildren().addAll(messageInput, sendBtn);

        chatView.setTop(topBar);
        chatView.setCenter(chatScrollPane);
        chatView.setBottom(inputArea);

        contentPane.getChildren().add(chatView);

        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void loadMessages(int convId) {
        String sql = "SELECT m.*, u.first_name, u.last_name, u.profile_picture FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.conversation_id = ? ORDER BY m.timestamp ASC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, convId);
            ResultSet rs = stmt.executeQuery();

            chatMessagesArea.getChildren().clear();
            int myId = SessionManager.getInstance().getCurrentUserId();
            boolean isGroup = isConversationGroup(convId);

            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                String content = rs.getString("content");
                String type = rs.getString("message_type");
                Timestamp time = rs.getTimestamp("timestamp");
                String senderName = rs.getString("first_name") + " " + rs.getString("last_name");
                String profilePic = rs.getString("profile_picture");

                HBox msgBox = createMessageBubble(content, senderId == myId, type, time, isGroup ? senderName : null, profilePic);
                chatMessagesArea.getChildren().add(msgBox);
            }

            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createMessageBubble(String content, boolean isMe, String type, Timestamp time, String senderName, String profilePic) {
        HBox container = new HBox(10);
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isMe && senderName != null) {
            // Show profile picture for group chat messages from others
            StackPane avatarPane = new StackPane();
            if (profilePic != null && !profilePic.isEmpty()) {
                ImageView avatar = ProfilePictureUtil.createCircularImageView(profilePic, 15);
                avatarPane.getChildren().add(avatar);
            } else {
                Circle avatar = new Circle(15);
                avatar.setFill(Color.web("#EC6D87"));
                avatarPane.getChildren().add(avatar);
            }
            container.getChildren().add(avatarPane);
        }

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setMaxWidth(400);

        // Show sender name in group chats (for messages from others)
        if (senderName != null && !isMe) {
            Label nameLabel = new Label(senderName);
            nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
            bubble.getChildren().add(nameLabel);
        }

        if (type.equals("payment")) {
            bubble.setStyle("-fx-background-color: #D1FAE5; -fx-background-radius: 15;");
            Label payLabel = new Label("💰 " + content);
            payLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #065F46;");
            bubble.getChildren().add(payLabel);
        } else {
            bubble.setStyle("-fx-background-color: " + (isMe ? "#EC6D87" : "#F3F4F6") + "; -fx-background-radius: 15;");
            Label contentLabel = new Label(content);
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-text-fill: " + (isMe ? "white" : "#1F2937") + ";");
            bubble.getChildren().add(contentLabel);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
        Label timeLabel = new Label(fmt.format(time));
        timeLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        bubble.getChildren().add(timeLabel);

        container.getChildren().add(bubble);
        return container;
    }

    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentConversationId == -1) return;

        SocketClient.getInstance().sendMessage(
                currentConversationId,
                SessionManager.getInstance().getCurrentUserId(),
                content,
                "text"
        );

        messageInput.clear();

        // Immediately reload messages to show your sent message
        // The socket event will update it for others
        Platform.runLater(() -> {
            try {
                Thread.sleep(100); // Small delay to let server process
                loadMessages(currentConversationId);
                loadConversations(); // Update last message in chat list
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showSendMoneyDialog() {
        int receiverId = getOtherUserId(currentConversationId);
        if (receiverId == -1) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Can only send money in 1-on-1 chats");
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Send Money");
        dialog.setHeaderText("Send money to " + getOtherUserName(currentConversationId));
        dialog.setContentText("Amount:");

        dialog.showAndWait().ifPresent(amount -> {
            try {
                double amt = Double.parseDouble(amount);
                if (amt > 0) {
                    SocketClient.getInstance().sendMoney(
                            SessionManager.getInstance().getCurrentUserId(),
                            receiverId,
                            amt,
                            currentConversationId
                    );

                    // Reload messages after a short delay to show the payment
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(200); // Wait for server to process
                            loadMessages(currentConversationId);
                            loadConversations();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid amount");
                alert.showAndWait();
            }
        });
    }

    // Socket event handlers
    public void handleNewMessage(JSONObject data) {
        try {
            int convId = data.getInt("conversationId");
            Platform.runLater(() -> {
                // Only reload messages if we're viewing this conversation
                if (convId == currentConversationId) {
                    loadMessages(convId);
                }
                // Always reload conversation list to update last message
                loadConversations();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleMoneyReceived(JSONObject data) {
        try {
            int convId = data.optInt("conversationId", -1);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "You received $" + data.optDouble("amount", 0));
                alert.showAndWait();

                // Reload the current conversation to show the payment message
                if (convId == currentConversationId) {
                    loadMessages(convId);
                }

                // Reload conversations list
                loadConversations();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleFriendStatusChanged(JSONObject data) {
        Platform.runLater(this::loadConversations);
    }

    public void handleNotification(JSONObject data) {
        // Show notification
    }

    public void handleFriendRequestResponse(JSONObject data) {
        try {
            boolean success = data.getBoolean("success");
            String message = data.getString("message");

            Platform.runLater(() -> {
                Alert alert = new Alert(
                        success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                        message
                );
                alert.setTitle(success ? "Success" : "Error");
                alert.setHeaderText(null);
                alert.showAndWait();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Friend functions
    private void showOnlineFriends() {
        showFriendsList("Online Friends", "SELECT u.* FROM users u JOIN friendships f ON u.id = f.friend_id WHERE f.user_id = ? AND u.is_online = true");
    }

    private void showAllFriends() {
        showFriendsList("All Friends", "SELECT u.* FROM users u JOIN friendships f ON u.id = f.friend_id WHERE f.user_id = ?");
    }

    private void showFriendsList(String title, String sql) {
        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox friendsList = new VBox(10);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int friendId = rs.getInt("id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String username = rs.getString("username");
                boolean online = rs.getBoolean("is_online");

                HBox item = createFriendItem(friendId, name, username, online);
                friendsList.getChildren().add(item);
            }

            if (friendsList.getChildren().isEmpty()) {
                Label empty = new Label("No friends yet");
                empty.setStyle("-fx-text-fill: #9CA3AF;");
                friendsList.getChildren().add(empty);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        ScrollPane scroll = new ScrollPane(friendsList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(titleLabel, scroll);
        contentPane.getChildren().setAll(view);
    }

    private HBox createFriendItem(int userId, String name, String username, boolean online) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");

        StackPane avatarPane = new StackPane();

        // Get profile picture
        String profilePic = ProfilePictureUtil.getProfilePicture(userId);
        if (profilePic != null && !profilePic.isEmpty()) {
            ImageView avatar = ProfilePictureUtil.createCircularImageView(profilePic, 30);
            avatarPane.getChildren().add(avatar);
        } else {
            Circle avatar = new Circle(30);
            avatar.setFill(Color.web("#EC6D87"));
            avatarPane.getChildren().add(avatar);
        }

        if (online) {
            Circle indicator = new Circle(10);
            indicator.setFill(Color.web("#10B981"));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
            avatarPane.getChildren().add(indicator);
        }

        VBox text = new VBox(3);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label usernameLabel = new Label("@" + username);
        usernameLabel.setStyle("-fx-text-fill: #6B7280;");
        text.getChildren().addAll(nameLabel, usernameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button msgBtn = new Button("Message");
        msgBtn.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand;");
        msgBtn.setOnAction(e -> startConversation(userId));

        item.getChildren().addAll(avatarPane, text, spacer, msgBtn);
        return item;
    }

    private void showAddFriendsView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setAlignment(Pos.TOP_CENTER);
        view.setStyle("-fx-background-color: white;");

        Label title = new Label("Add Friends");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setMaxWidth(500);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username or email");
        usernameField.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-padding: 12;");
        HBox.setHgrow(usernameField, Priority.ALWAYS);

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-font-size: 14px; -fx-padding: 10 0 0 0;");

        Button sendBtn = new Button("Send Request");
        sendBtn.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 12 20; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                SocketClient.getInstance().sendFriendRequest(
                        SessionManager.getInstance().getCurrentUserId(),
                        username
                );
                msgLabel.setText("Sending friend request...");
                msgLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                usernameField.clear();
            } else {
                msgLabel.setText("Please enter a username or email");
                msgLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
            }
        });

        inputBox.getChildren().addAll(usernameField, sendBtn);
        view.getChildren().addAll(title, inputBox, msgLabel);
        contentPane.getChildren().setAll(view);
    }

    private void showPendingRequests() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: white;");

        Label title = new Label("Pending Requests");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox requestsList = new VBox(10);

        String sql = "SELECT fr.id, fr.sender_id, u.first_name, u.last_name, u.username FROM friend_requests fr " +
                "JOIN users u ON fr.sender_id = u.id WHERE fr.receiver_id = ? AND fr.status = 'pending'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int reqId = rs.getInt("id");
                int senderId = rs.getInt("sender_id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String username = rs.getString("username");

                HBox item = createPendingRequestItem(reqId, senderId, name, username);
                requestsList.getChildren().add(item);
            }

            if (requestsList.getChildren().isEmpty()) {
                Label empty = new Label("No pending requests");
                empty.setStyle("-fx-text-fill: #9CA3AF;");
                requestsList.getChildren().add(empty);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        ScrollPane scroll = new ScrollPane(requestsList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, scroll);
        contentPane.getChildren().setAll(view);
    }

    private HBox createPendingRequestItem(int requestId, int senderId, String name, String username) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");

        // Get profile picture
        String profilePic = ProfilePictureUtil.getProfilePicture(senderId);
        if (profilePic != null && !profilePic.isEmpty()) {
            ImageView avatar = ProfilePictureUtil.createCircularImageView(profilePic, 30);
            item.getChildren().add(avatar);
        } else {
            Circle avatar = new Circle(30);
            avatar.setFill(Color.web("#EC6D87"));
            item.getChildren().add(avatar);
        }

        VBox text = new VBox(3);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label usernameLabel = new Label("@" + username);
        usernameLabel.setStyle("-fx-text-fill: #6B7280;");
        text.getChildren().addAll(nameLabel, usernameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand;");
        acceptBtn.setOnAction(e -> {
            SocketClient.getInstance().respondToFriendRequest(requestId, true);

            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), item);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(event -> {
                item.setVisible(false);
                item.setManaged(false);
                loadConversations();
            });
            fade.play();
        });

        Button rejectBtn = new Button("Reject");
        rejectBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            SocketClient.getInstance().respondToFriendRequest(requestId, false);

            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), item);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(event -> {
                item.setVisible(false);
                item.setManaged(false);
            });
            fade.play();
        });

        item.getChildren().addAll(text, spacer, acceptBtn, rejectBtn);
        return item;
    }

    private void showCreateGroupView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: white;");

        Label title = new Label("Create Group");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField groupName = new TextField();
        groupName.setPromptText("Group Name");
        groupName.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-padding: 12;");

        VBox friendsList = new VBox(10);
        List<Integer> selected = new ArrayList<>();

        String sql = "SELECT u.id, u.first_name, u.last_name FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id WHERE f.user_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int friendId = rs.getInt("id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");

                CheckBox cb = new CheckBox(name);
                cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) selected.add(friendId);
                    else selected.remove(Integer.valueOf(friendId));
                });
                friendsList.getChildren().add(cb);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        ScrollPane scroll = new ScrollPane(friendsList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #F3F4F6; -fx-padding: 12 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> showDefaultView());

        Button createBtn = new Button("Create Group");
        createBtn.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; -fx-padding: 12 20; -fx-background-radius: 8; -fx-cursor: hand;");
        createBtn.setOnAction(e -> {
            if (!groupName.getText().trim().isEmpty() && !selected.isEmpty()) {
                createGroup(groupName.getText().trim(), selected);
                loadConversations();
                showDefaultView();
            }
        });

        buttons.getChildren().addAll(cancelBtn, createBtn);

        view.getChildren().addAll(title, groupName, new Label("Select Friends:"), scroll, buttons);
        contentPane.getChildren().setAll(view);
    }

    private void createGroup(String name, List<Integer> memberIds) {
        String createSql = "INSERT INTO conversations (name, is_group, created_by) VALUES (?, true, ?) RETURNING id";
        String addMemberSql = "INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(createSql)) {

            stmt.setString(1, name);
            stmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int convId = rs.getInt("id");

                PreparedStatement addStmt = conn.prepareStatement(addMemberSql);
                addStmt.setInt(1, convId);
                addStmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
                addStmt.executeUpdate();

                for (int memberId : memberIds) {
                    addStmt.setInt(1, convId);
                    addStmt.setInt(2, memberId);
                    addStmt.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startConversation(int userId) {
        String checkSql = "SELECT c.id FROM conversations c " +
                "JOIN conversation_members cm1 ON c.id = cm1.conversation_id " +
                "JOIN conversation_members cm2 ON c.id = cm2.conversation_id " +
                "WHERE cm1.user_id = ? AND cm2.user_id = ? AND c.is_group = false";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                openChat(rs.getInt("id"));
            } else {
                // Create new conversation
                String createSql = "INSERT INTO conversations (is_group) VALUES (false) RETURNING id";
                PreparedStatement createStmt = conn.prepareStatement(createSql);
                ResultSet createRs = createStmt.executeQuery();

                if (createRs.next()) {
                    int newConvId = createRs.getInt("id");

                    String addMemberSql = "INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)";
                    PreparedStatement addStmt = conn.prepareStatement(addMemberSql);

                    addStmt.setInt(1, newConvId);
                    addStmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
                    addStmt.executeUpdate();

                    addStmt.setInt(1, newConvId);
                    addStmt.setInt(2, userId);
                    addStmt.executeUpdate();

                    loadConversations();
                    openChat(newConvId);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper methods
    private String getConversationName(int convId) {
        String sql = "SELECT name, is_group FROM conversations WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, convId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getBoolean("is_group")) {
                    return rs.getString("name");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getOtherUserName(convId);
    }

    private String getOtherUserName(int convId) {
        String sql = "SELECT u.first_name, u.last_name FROM users u " +
                "JOIN conversation_members cm ON u.id = cm.user_id " +
                "WHERE cm.conversation_id = ? AND u.id != ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, convId);
            stmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private int getOtherUserId(int convId) {
        String sql = "SELECT user_id FROM conversation_members WHERE conversation_id = ? AND user_id != ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, convId);
            stmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean isUserOnline(int userId) {
        if (userId == -1) return false;

        String sql = "SELECT is_online FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getBoolean("is_online");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isConversationGroup(int convId) {
        String sql = "SELECT is_group FROM conversations WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, convId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_group");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showGroupMembers(int conversationId) {
        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: white;");

        Label title = new Label("Group Members");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox membersList = new VBox(15);

        String sql = "SELECT u.id, u.first_name, u.last_name, u.username, u.profile_picture, u.is_online " +
                "FROM users u JOIN conversation_members cm ON u.id = cm.user_id " +
                "WHERE cm.conversation_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int memberId = rs.getInt("id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String username = rs.getString("username");
                String profilePic = rs.getString("profile_picture");
                boolean online = rs.getBoolean("is_online");

                HBox memberItem = createGroupMemberItem(memberId, name, username, profilePic, online);
                membersList.getChildren().add(memberItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        ScrollPane scroll = new ScrollPane(membersList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button backBtn = new Button("Back to Chat");
        backBtn.setStyle("-fx-background-color: #EC6D87; -fx-text-fill: white; -fx-padding: 12 20; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setOnAction(e -> openChat(conversationId));

        view.getChildren().addAll(title, scroll, backBtn);
        contentPane.getChildren().setAll(view);
    }

    private HBox createGroupMemberItem(int userId, String name, String username, String profilePic, boolean online) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");

        StackPane avatarPane = new StackPane();

        // Show profile picture
        if (profilePic != null && !profilePic.isEmpty()) {
            ImageView avatar = ProfilePictureUtil.createCircularImageView(profilePic, 30);
            avatarPane.getChildren().add(avatar);
        } else {
            Circle avatar = new Circle(30);
            avatar.setFill(Color.web("#EC6D87"));
            avatarPane.getChildren().add(avatar);
        }

        // Online indicator
        if (online) {
            Circle indicator = new Circle(10);
            indicator.setFill(Color.web("#10B981"));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
            avatarPane.getChildren().add(indicator);
        }

        VBox text = new VBox(3);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label usernameLabel = new Label("@" + username);
        usernameLabel.setStyle("-fx-text-fill: #6B7280;");

        // Add "You" indicator if it's the current user
        if (userId == SessionManager.getInstance().getCurrentUserId()) {
            Label youLabel = new Label("(You)");
            youLabel.setStyle("-fx-text-fill: #EC6D87; -fx-font-size: 12px;");
            text.getChildren().addAll(nameLabel, usernameLabel, youLabel);
        } else {
            text.getChildren().addAll(nameLabel, usernameLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(avatarPane, text, spacer);
        return item;
    }




    private void markMessagesAsRead(int convId) {
        String sql = "UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id != ? AND is_read = false";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, convId);
            stmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
            stmt.executeUpdate();

            // Don't reload conversations here - just update the specific chat item
            updateChatItemReadStatus(convId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChatItemReadStatus(int convId) {
        for (javafx.scene.Node node : chatList.getChildren()) {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                // Check if this is the right chat item (you can store convId in userData)
                if (item.getUserData() != null && item.getUserData().equals(convId)) {
                    // Remove the unread badge if it exists
                    if (item.getChildren().size() > 2) {
                        item.getChildren().remove(2); // Remove badge
                    }
                    // Update text styles to non-bold
                    VBox textBox = (VBox) item.getChildren().get(1);
                    Label nameLabel = (Label) textBox.getChildren().get(0);
                    Label msgLabel = (Label) textBox.getChildren().get(1);
                    nameLabel.setStyle("-fx-font-weight: normal; -fx-font-size: 14px; -fx-text-fill: #1F2937;");
                    msgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
                    break;
                }
            }
        }
    }







}