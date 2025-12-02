package model;


import utils.Validators;

public class User {
    private long id;
    private String username;
    private String email;
    private String password;

    // for get User
    public User(long id, String username, String email, String password) {
        this.id = id;
        this.username = Validators.validateUsername(username);
        this.email = Validators.validateEmail(email);
        this.password = Validators.validatePassword(password);
    }

    // for post, delete, update User
    public User(String username, String email, String password) {
        this.username = Validators.validateUsername(username);
        this.email = Validators.validateEmail(email);
        this.password = Validators.validatePassword(password);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
