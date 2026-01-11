package com.githubapi.zadanie;

class UserNotFoundException extends RuntimeException {

    UserNotFoundException(String username) {
        super("User not found: " + username);
    }
}
