# TaskFlow

Sistema web de gerenciamento de tarefas, hábitos e tarefas recorrentes, com dashboard de desempenho, gráficos e mapa de calor (heatmap). Backend em **Java** com API REST, **MySQL** como banco de dados e frontend em **HTML, CSS e JavaScript**.

## Visão geral

- Autenticação real (cadastro e login) com senhas armazenadas como hash (SHA-256 com salt).
- CRUD de tarefas, hábitos e tarefas recorrentes.
- Marcação de conclusão/falha e histórico de contribuições.
- Dashboard com gráficos (barras, linha, rosca) e heatmap dos últimos meses.
- Arquitetura em camadas (Controller → Service → Repository) para separar responsabilidades.

```
Frontend (web/)  →  HTTP/JSON  →  Controller  →  Service  →  Repository  →  MySQL
```

## Como executar

### Requisitos

- JDK 17+
- Maven 3.8+
- MySQL 8
- Git
- *(opcional)* VS Code com Live Server para servir o frontend

### Passos

```bash
# 1. Clonar e compilar
git clone <https://github.com/V341551M0/TaskFlow>
cd TaskFlow
mvn compile

# 2. Subir MySQL + API (porta 8080)
./run.sh

# 3. Abrir o frontend (Live Server) em:
# http://127.0.0.1:5501/web/pages/login.html
```

Cadastre um usuário (informe nome, e-mail e senha) e explore a aplicação. O login aceita **e-mail ou nome de usuário**.

> O `run.sh` detecta o MySQL a usar: prefere o **MySQL do sistema** (`127.0.0.1:3306`, persistente) quando acessível e, caso contrário, sobe uma instância isolada em `/tmp/taskflow-mysql` na porta `3307` (o AppArmor do Ubuntu restringe o `mysqld`). Para dados persistentes, configure o usuário no MySQL do sistema — ver [docs/setup/database.md](docs/setup/database.md).

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 17, `HttpServer` do JDK, JDBC |
| Banco de dados | MySQL 8 |
| Frontend | HTML, CSS, JavaScript (sem framework) |
| Build/Testes | Maven, JUnit 5 |

## Principais funcionalidades

- Cadastro e login de usuários conectados ao banco de dados
- Tarefas, hábitos e tarefas recorrentes (criar, concluir, falhar, excluir)
- Dashboard com gráficos de desempenho semanal, mensal e de distribuição
- Mapa de calor (heatmap) com histórico de conclusões e falhas

## Documentação

- [→ Arquitetura](docs/architecture/overview.md) — visão geral, camadas e decisões
- [→ API](docs/api/authentication.md) — endpoints de autenticação e itens
- [→ Banco de dados](docs/database/schema.md) — tabelas e relacionamentos
- [→ Configuração](docs/setup/development.md) — preparando o ambiente do zero