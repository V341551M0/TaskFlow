package dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDto {
    private String id;
    private String username;
    private String email;
    private String date;
    private String password;

    public UserDto(String id, String username, String email, String date) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDate() {
        return date;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}