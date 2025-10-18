package com.birdsenger;

import org.mindrot.jbcrypt.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        String password = "password123";
        String storedHash = "$2a$10$8K1p/a0dL3.kyTRHb7hYG.XOFv4dQqH.vq7dAcgXh.5WqPQPj7.8W";

        System.out.println("Testing BCrypt:");
        System.out.println("Password: " + password);
        System.out.println("Stored Hash: " + storedHash);
        System.out.println("Match: " + BCrypt.checkpw(password, storedHash));

        // Generate new hash
        String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("\nNew hash for 'password123': " + newHash);
        System.out.println("New hash works: " + BCrypt.checkpw(password, newHash));
    }
}