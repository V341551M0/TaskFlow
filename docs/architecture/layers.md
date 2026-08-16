# Camadas da Arquitetura

O projeto segue uma arquitetura em camadas. A regra principal é: cada camada conhece apenas a camada imediatamente abaixo dela, nunca acima.

```
Controller  →  Service  →  Repository  →  MySQL
     ↓             ↓            ↓
   HTTP        negócio      persistência
```

## Controller (HTTP)

**Pacote:** `controller`

- Recebe a requisição HTTP (`HttpExchange` do `com.sun.net.httpserver`).
- Interpreta o método (GET/POST/OPTIONS) e o corpo JSON.
- Delega para o Service e traduz o resultado em códigos de status e JSON.
- Trata CORS para permitir o consumo pelo frontend.

Exemplos: `TaskController` (itens e dashboards), `AuthController` (login e cadastro).

> Não contém regra de negócio. Se o Controller precisar "pensar", a lógica deve descer para o Service.

## Service (regras de negócio)

**Pacote:** `service`

- Validações de entrada (ex.: campos obrigatórios, senha confirmada).
- Regras de domínio (ex.: item finalizado não pode mudar de status; e-mail não pode ser duplicado).
- Orquestra o Repository e devolve objetos prontos para o Controller.

Exemplos: `TaskService`, `UserService`.

## Repository (persistência)

**Pacote:** `repository`

- Executa SQL usando `PreparedStatement` (parâmetros vinculados, imune a SQL Injection).
- Mapeia linhas do `ResultSet` para DTOs.
- Esconde do restante da aplicação qual banco é usado e como.

Exemplos: `TaskRepository`, `UserRepository`.

## DTO (transporte de dados)

**Pacote:** `dto`

- Representa os dados que atravessam as camadas (e que vão/voltam da API).
- Nunca contém lógica; apenas campos e acessores.

Exemplos: `TaskDto`, `UserDto`.

> Nota de segurança: `UserDto` carrega o hash da senha internamente, mas a serialização JSON nunca expõe esse campo.

## Utilitários (apoio transversal)

| Classe | Responsabilidade |
|---|---|
| `util.DatabaseConnection` | Cria conexões JDBC; executa `db/schema.sql` automaticamente no primeiro uso. |
| `util.Json` | Parse de corpo JSON e serialização de respostas (Map/List/DTO). |
| `util.PasswordUtil` | Hash SHA-256 com salt e verificação em tempo constante. |
| `dto.TaskDto` / `dto.UserDto` | Contratos de dados das camadas de serviço e persistência. |