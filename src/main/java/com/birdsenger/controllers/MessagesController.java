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
                "(SELECT content FROM messages WHERE conversation_id = c.id ORDER BY timestamp DESC LIMIT 1) as last_msg " +
                "FROM conversations c " +
                "JOIN conversation_members cm ON c.id = cm.conversation_id " +
                "WHERE cm.user_id = ? " +
                "ORDER BY c.id DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            chatList.getChildren().clear();

            while (rs.next()) {
                int convId = rs.getInt("id");
                boolean isGroup = rs.getBoolean("is_group");
                String name = isGroup ? rs.getString("name") : getOtherUserName(convId);
                String lastMsg = rs.getString("last_msg");
                boolean online = !isGroup && isUserOnline(getOtherUserId(convId));

                HBox chatItem = createChatItem(convId, name, lastMsg != null ? lastMsg : "", online);
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

    private HBox createChatItem(int convId, String name, String lastMsg, boolean online) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
        item.getStyleClass().add("chat-item");

        StackPane avatarPane = new StackPane();
        Circle avatar = new Circle(25);
        avatar.setFill(Color.web("#EC6D87"));

        if (online) {
            Circle indicator = new Circle(8);
            indicator.setFill(Color.web("#10B981"));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
            avatarPane.getChildren().addAll(avatar, indicator);
        } else {
            avatarPane.getChildren().add(avatar);
        }

        VBox textBox = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label msgLabel = new Label(lastMsg.length() > 40 ? lastMsg.substring(0, 40) + "..." : lastMsg);
        msgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        textBox.getChildren().addAll(nameLabel, msgLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        item.getChildren().addAll(avatarPane, textBox);
        item.setOnMouseClicked(e -> openChat(convId));

        return item;
    }

    private void openChat(int conversationId) {
        currentConversationId = conversationId;
        contentPane.getChildren().clear();

        BorderPane chatView = new BorderPane();
        chatView.setStyle("-fx-background-color: white;");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        Label chatName = new Label(getConversationName(conversationId));
        chatName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button moneyBtn = new Button("💰 Send Money");
        moneyBtn.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        moneyBtn.setOnAction(e -> showSendMoneyDialog());

        topBar.getChildren().addAll(chatName, spacer, moneyBtn);

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
        String sql = "SELECT m.*, u.first_name, u.last_name FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.conversation_id = ? ORDER BY m.timestamp ASC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, convId);
            ResultSet rs = stmt.executeQuery();

            chatMessagesArea.getChildren().clear();
            int myId = SessionManager.getInstance().getCurrentUserId();

            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                String content = rs.getString("content");
                String type = rs.getString("message_type");
                Timestamp time = rs.getTimestamp("timestamp");

                HBox msgBox = createMessageBubble(content, senderId == myId, type, time);
                chatMessagesArea.getChildren().add(msgBox);
            }

            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createMessageBubble(String content, boolean isMe, String type, Timestamp time) {
        HBox container = new HBox();
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setMaxWidth(400);

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
                if (convId == currentConversationId) {
                    loadMessages(convId);
                }
                loadConversations();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMoneyReceived(JSONObject data) {
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "You received $" + data.optDouble("amount", 0));
                alert.showAndWait();
                if (currentConversationId != -1) {
                    loadMessages(currentConversationId);
                }
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
        Circle avatar = new Circle(30);
        avatar.setFill(Color.web("#EC6D87"));

        if (online) {
            Circle indicator = new Circle(10);
            indicator.setFill(Color.web("#10B981"));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
            avatarPane.getChildren().addAll(avatar, indicator);
        } else {
            avatarPane.getChildren().add(avatar);
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

        Circle avatar = new Circle(30);
        avatar.setFill(Color.web("#EC6D87"));

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

        item.getChildren().addAll(avatar, text, spacer, acceptBtn, rejectBtn);
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
}