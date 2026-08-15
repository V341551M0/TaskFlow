package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import dto.UserDto;
import util.DatabaseConnection;

public class UserRepository {

    public UserDto save(String username, String passwordHash) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String date = LocalDate.now().toString();
        String sql = """
                INSERT INTO `user` (id, `user`, data, senha)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, username);
            statement.setString(3, date);
            statement.setString(4, passwordHash);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível criar o usuário", ex);
        }

        return new UserDto(id, username, date);
    }

    public UserDto findByUsername(String username) {
        String sql = "SELECT id, `user`, data, senha FROM `user` WHERE `user` = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                UserDto dto = new UserDto(
                        resultSet.getString("id"),
                        resultSet.getString("user"),
                        resultSet.getString("data")
                );
                dto.setPassword(resultSet.getString("senha"));
                return dto;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível buscar o usuário", ex);
        }
    }
}
