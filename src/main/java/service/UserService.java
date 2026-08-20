package service;

import dto.UserDto;
import repository.UserRepository;
import util.PasswordUtil;

public class UserService {
    private final UserRepository repository;

    public UserService() {
        this.repository = new UserRepository();
    }

    public UserDto register(String name, String email, String password) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Informe um nome de usuário.");
        }
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }

        UserDto existing = repository.findByIdentifier(normalizedEmail);
        if (existing != null && normalizedEmail.equalsIgnoreCase(existing.getEmail())) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }
        if (repository.findByIdentifier(normalizedName) != null) {
            throw new IllegalStateException("Este nome de usuário já está em uso.");
        }

        return repository.save(normalizedName, normalizedEmail, PasswordUtil.hash(password));
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }

    public UserDto login(String identifier, String password) {
        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Informe e-mail ou nome de usuário e senha.");
        }
        UserDto user = repository.findByIdentifier(identifier.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            throw new IllegalArgumentException("E-mail, nome de usuário ou senha inválidos.");
        }
        return user;
    }
}
