# Setup de Desenvolvimento

Guia para rodar o TaskFlow localmente do zero.

## Requisitos

- **JDK 17+** (o projeto compila para Java 17)
- **Maven 3.8+**
- **MySQL 8** (o app usa `mysql-connector-j`) — **obrigatório** no sistema, em `127.0.0.1:3306`
- **Git**
- *(opcional)* VS Code com a extensão **Live Server** para servir o frontend

> O backend usa apenas o `HttpServer` embutido do JDK — **não** é necessário instalar Tomcat nem Spring Boot.
>
> O TaskFlow usa **exclusivamente o MySQL do sistema**. Não há instância alternativa em `/tmp`; se o MySQL estiver parado, o `run.sh` informa o problema em vez de criar outro banco.

## Passo a passo

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd TaskFlow
```

### 2. Compilar

```bash
mvn compile
```

### 3. Subir a infraestrutura (MySQL + API)

Prepare o MySQL do sistema uma única vez (cria os bancos e o usuário `taskflow`):

```bash
sudo mysql < db/schema.sql
```

Depois, inicie a API com o script (conecta no MySQL do sistema em `127.0.0.1:3306` com `taskflow`/`TaskFlow@2026` e sobe a API na porta 8080):

```bash
./run.sh
```

Se o MySQL não estiver acessível, o `run.sh` encerra com erro e orienta a iniciar o serviço:

```bash
sudo systemctl start mysql
```

> **Nota sobre AppArmor (Ubuntu):** não é necessário — o banco vive no MySQL do sistema (`/var/lib/mysql`), não em `/tmp`.

Ou, manualmente (com o MySQL do sistema já configurado):

```bash
mvn exec:java
```

### 4. Rodar o frontend

O frontend é estático. Com o Live Server do VS Code:

1. Abra a pasta `web/` (ou a raiz do projeto) no Live Server (porta padrão 5501).
2. Acesse `http://127.0.0.1:5501/web/pages/login.html`.
3. Cadastre um usuário — o e-mail vira o login — e entre na tela inicial.

> A URL da API é definida em `web/config.js` (`window.TASKFLOW_API_URL`): em desenvolvimento aponta para `http://localhost:8080`; em produção, mantenha vazio (mesma origem) ou aponte para o backend. Todas as páginas aplicam CSP e o backend restringe o CORS às origens em `CORS_ALLOWED_ORIGINS` (padrão dev: `http://localhost:5501` e `http://127.0.0.1:5501`).

## Variáveis de ambiente

O `run.sh` carrega um arquivo `.env` na raiz do projeto (ignorado pelo Git) se existir, e pode sobrescrever qualquer variável abaixo. Em produção, defina as variáveis explicitamente no ambiente — **não dependa dos padrões de desenvolvimento**.

| Variável | Padrão | Descrição |
|---|---|---|
| `MYSQL_HOST` | `127.0.0.1` | Host do MySQL do sistema. |
| `MYSQL_PORT` | `3306` | Porta do MySQL do sistema. |
| `MYSQL_DB` | `taskflow_db` | Nome do banco (criado automaticamente). |
| `MYSQL_USER` | `taskflow` | Usuário da aplicação. |
| `MYSQL_PASSWORD` | `TaskFlow@2026` | Senha padrão de **desenvolvimento** (use outra em produção). |
| `JWT_SECRET` | *(sem padrão — obrigatória)* | Segredo de assinatura HS256 do JWT (mín. 32 caracteres). Se ausente, o `run.sh` gera uma e salva no `.env`; a aplicação **recusa iniciar** sem ela. |
| `PORT` | `8080` | Porta da API (via `server.port` ou `PORT`). |

Os mesmos padrões são usados por `run.sh` e por `util/DatabaseConnection`.

## Testes

```bash
JWT_SECRET=$(openssl rand -hex 32) mvn test
```

> **Atenção:** a suíte usa o banco `taskflow_test` (variável `MYSQL_DB=taskflow_test` configurada no surefire) e limpa as tabelas a cada teste (centralizado em `util.TestDbSupport`). O usuário `taskflow` precisa ter acesso a esse banco — ele é criado por `db/schema.sql`. O `JWT_SECRET` é repassado pelo surefire a partir do ambiente — exporte-a (como acima) para os testes que exercitam a autenticação.

> **CI / isolamento:** para garantir isolamento total entre suíte e base de desenvolvimento, o ideal é rodar os testes contra um MySQL via **Testcontainers** (exige o usuário no grupo `docker`). Enquanto o ambiente local não tiver esse acesso, os testes usam o banco dedicado `taskflow_test`, limpo por teste. Detalhes em [migrations.md](../database/migrations.md).

## Estrutura do projeto

```
TaskFlow/
├── db/                     # schema.sql (bootstrap: bancos e usuário) — as tabelas vêm das migrações Flyway
├── docs/                   # documentação (esta pasta)
├── src/main/java/          # backend Java (app, controller, service, repository, dto, util)
├── src/test/java/          # testes
├── web/                    # frontend estático (HTML/CSS/JS)
├── run.sh                  # conecta no MySQL do sistema e sobe a API
└── pom.xml
```