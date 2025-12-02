package utils;


public class Validators {

    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("username mustn't be empty");
        return username;
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("email mustn't be empty");
        if (!email.contains("@")) throw new IllegalArgumentException("Illegal email");
        return email;
    }

    public static String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("password mustn't be empty");
        if (password.length() < 8) throw new IllegalArgumentException("password must be longer than 7 characters");
        return password;
    }
}
