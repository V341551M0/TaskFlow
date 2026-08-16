# Setup de Desenvolvimento

Guia para rodar o TaskFlow localmente do zero.

## Requisitos

- **JDK 17+** (o projeto compila para Java 17)
- **Maven 3.8+**
- **MySQL 8** (o app usa `mysql-connector-j`)
- **Git**
- *(opcional)* VS Code com a extensão **Live Server** para servir o frontend

> O backend usa apenas o `HttpServer` embutido do JDK — **não** é necessário instalar Tomcat nem Spring Boot.

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

A forma mais simples é usar o script que já cuida de tudo (cria/sobe um MySQL isolado e inicia a API na porta 8080):

```bash
./run.sh
```

> **Nota sobre AppArmor (Ubuntu):** o perfil do `mysqld` só permite dados em `/tmp` e `/var/lib/mysql`. Por isso o script usa uma instância isolada em `/tmp/taskflow-mysql`. Como `/tmp` é apagado em reinicializações, **rode `./run.sh` de novo após reiniciar o PC**. Alternativa permanente: configurar um usuário no MySQL do sistema (ver [database.md](database.md)).

Ou, manualmente:

```bash
# terminal 1 — API
export MYSQL_HOST=127.0.0.1 MYSQL_PORT=3307 MYSQL_USER=root MYSQL_PASSWORD=root
mvn exec:java

# terminal 2 — MySQL isolado (se o run.sh não estiver em uso)
mysqld --no-defaults --datadir=/tmp/taskflow-mysql/data \
  --socket=/tmp/taskflow-mysql/mysql.sock --port=3307 \
  --bind-address=127.0.0.1 --user=$(whoami)
```

### 4. Rodar o frontend

O frontend é estático. Com o Live Server do VS Code:

1. Abra a pasta `web/` (ou a raiz do projeto) no Live Server (porta padrão 5501).
2. Acesse `http://127.0.0.1:5501/web/pages/login.html`.
3. Cadastre um usuário — o e-mail vira o login — e entre na tela inicial.

> O frontend chama a API em `http://localhost:8080`. O backend já envia os cabeçalhos CORS necessários.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `MYSQL_HOST` | `localhost` | Host do MySQL. |
| `MYSQL_PORT` | `3306` | Porta do MySQL. |
| `MYSQL_DB` | `taskflow_db` | Nome do banco (criado automaticamente). |
| `MYSQL_USER` | `root` | Usuário do banco. |
| `MYSQL_PASSWORD` | `root` | Senha do banco. |
| `PORT` | `8080` | Porta da API (via `server.port` ou `PORT`). |

## Testes

```bash
mvn test
```

> **Atenção:** a suíte usa o MySQL real e os testes compartilham datas fixas, então rodar repetidamente no mesmo banco pode acumular valores no heatmap. Para um resultado limpo, rode contra um banco recém-criado.

## Estrutura do projeto

```
TaskFlow/
├── db/                     # schema.sql (executável)
├── docs/                   # documentação (esta pasta)
├── src/main/java/          # backend Java (app, controller, service, repository, dto, util)
├── src/test/java/          # testes
├── web/                    # frontend estático (HTML/CSS/JS)
├── run.sh                  # sobe MySQL isolado + API
└── pom.xml
```