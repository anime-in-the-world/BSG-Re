package com.birdsenger.utils;

import com.birdsenger.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Session started for: " + user.getFullName());
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("👋 Session ended for: " + currentUser.getFullName());
        }
        currentUser = null;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}