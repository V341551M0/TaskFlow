package dto;

public class UserDto {
    private String id;
    private String username;
    private String date;
    private String password;

    public UserDto() {
    }

    public UserDto(String id, String username, String date) {
        this.id = id;
        this.username = username;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
