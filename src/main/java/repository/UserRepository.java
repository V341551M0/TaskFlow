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

    public UserDto save(String username, String email, String passwordHash) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String date = LocalDate.now().toString();
        String sql = """
                INSERT INTO usuario (id, `user`, email, data, senha)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, date);
            statement.setString(5, passwordHash);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível criar o usuário", ex);
        }

        return new UserDto(id, username, email, date);
    }

    public UserDto findByIdentifier(String identifier) {
        String sql = "SELECT id, `user`, email, data, senha FROM usuario WHERE email = ? OR `user` = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                UserDto dto = new UserDto(
                        resultSet.getString("id"),
                        resultSet.getString("user"),
                        resultSet.getString("email"),
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
