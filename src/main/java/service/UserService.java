package service;

import dto.UserDto;
import repository.UserRepository;
import util.PasswordUtil;

public class UserService {
    private final UserRepository repository;

    public UserService() {
        this.repository = new UserRepository();
    }

    public UserDto register(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Informe uma senha.");
        }
        String normalizedUsername = username.trim();
        if (repository.findByUsername(normalizedUsername) != null) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }
        return repository.save(normalizedUsername, PasswordUtil.hash(password));
    }

    public UserDto login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Informe e-mail e senha.");
        }
        UserDto user = repository.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }
        return user;
    }
}
