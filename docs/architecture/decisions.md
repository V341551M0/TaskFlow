# Decisões Arquiteturais

Este documento registra as principais decisões de projeto e o motivo de cada uma. Serve como referência para entender *por que* o sistema é estruturado assim.

## 1. Arquitetura em camadas

**Decisão:** separar o código em Controller → Service → Repository → DTO.

**Motivo:** isolar responsabilidades (apresentação, regras de negócio e persistência). Isso torna o código testável (cada camada pode ser testada isoladamente), facilita a manutenção e demonstra boas práticas em um projeto de portfólio.

## 2. Servidor HTTP embutido do JDK (sem Spring Boot)

**Decisão:** usar `com.sun.net.httpserver.HttpServer`, que já vem no JDK.

**Motivo:** manter o projeto leve e com dependências mínimas (driver MySQL, Jackson, Flyway e JUnit). A troca por um framework (Spring Boot, por exemplo) não exige reescrever as camadas internas — apenas a camada Controller — o que reforça o benefício da separação em camadas.

## 3. Frontend estático sem framework

**Decisão:** HTML, CSS e JavaScript puros servidos como arquivos estáticos.

**Motivo:** escopo e simplicidade. O frontend consome a API via `fetch` com CORS liberado, sem etapa de build. Uma evolução possível seria adicionar um bundler ou framework SPA.

## 4. Schema gerenciado por migrações Flyway

**Decisão:** `util.DatabaseConnection.initialize()` executa **Flyway** no startup, aplicando migrações SQL versionadas em `src/main/resources/db/migration/` (`V1__create_tables.sql`, ...). Com `baselineOnMigrate=false`, qualquer falha ou banco não vazio sem histórico **aborta** a inicialização — a aplicação nunca executa `ALTER` ad-hoc nem atribui/exclui dados sem dono automaticamente.

**Motivo:** evolução controlada e idempotente do schema em todos os ambientes, com histórico auditable. O `db/schema.sql` ficou restrito ao **bootstrap** (bancos e usuário, executado com `sudo` uma única vez); as tabelas vêm das migrações. Detalhes em [database/migrations.md](../database/migrations.md).

## 5. Senhas com hash (SHA-256 + salt)

**Decisão:** `util.PasswordUtil` gera um salt aleatório por usuário e armazena `salt:hash` na coluna `senha`. A verificação usa comparação em tempo constante.

**Motivo:** nunca armazenar senha em texto puro. A serialização JSON do `UserDto` nunca expõe o hash.

## 6. CORS restrito às origens permitidas

**Decisão:** o backend responde com `Access-Control-Allow-Origin` apenas quando a origem da requisição está na lista `CORS_ALLOWED_ORIGINS` (variável de ambiente, separada por vírgula). O padrão de desenvolvimento é `http://localhost:5501` e `http://127.0.0.1:5501` (Live Server); em produção, defina a origem do frontend. Nunca usa `*`.

**Motivo:** em desenvolvimento o frontend (Live Server na porta 5501) e a API (porta 8080) rodam em origens diferentes, então o CORS é necessário; restringir as origens evita que qualquer site possa chamar a API com o token do usuário.

## 7. Autenticação JWT validada no backend

**Decisão:** login/cadastro usam hash **PBKDF2** (`util.PasswordUtil`) e o backend emite um **JWT** (`util.TokenService`). O frontend guarda o token em `localStorage` e o envia no cabeçalho `Authorization`; o `util.AuthFilter` valida o token em todas as rotas `/api/*` (exceto `/api/auth/*`), e o `AuthContext` (ThreadLocal) propaga o usuário para as camadas. Dados sempre filtrados por `user_id` no service.

**Motivo:** o backend autentica de verdade contra o banco e garante isolamento por usuário. Evolução prevista: cookie HttpOnly (em vez de `localStorage`) e revogação/rotação de tokens.

## 8. Item genérico com três tabelas e heatmap desnormalizado

**Decisão:** tarefas, hábitos e tarefas recorrentes vivem em tabelas separadas porém com a mesma estrutura; o histórico de conclusões é normalizado em `item_history` e agregado em `daily_heatmap`.

**Motivo:** simplicidade de consulta por tipo e leitura rápida do heatmap (sem agregação a cada request). O custo é a duplicação estrutural entre as três tabelas — aceitável para o escopo atual.

## 9. SQL com `PreparedStatement`

**Decisão:** todas as consultas usam parâmetros vinculados.

**Motivo:** prevenção de SQL Injection e melhor legibilidade em relação à concatenação de strings.

## 10. Script de inicialização (`run.sh`)

**Decisão:** um único script sobe a infraestrutura local de forma idempotente usando **exclusivamente o MySQL do sistema** em `127.0.0.1:3306` (banco `taskflow_db`, usuário `taskflow`). Não há instância alternativa em `/tmp`: se o MySQL não estiver acessível, o script encerra com erro e orienta a iniciar o serviço (ex.: `sudo systemctl start mysql`).

**Motivo:** o banco do sistema é persistente e sobrevive a reinicializações. Um fallback em `/tmp/taskflow-mysql` foi abandonado porque `/tmp` é apagado no reboot, fazendo o TaskFlow iniciar com um banco vazio diferente do banco persistente (perda aparente de dados). Detalhes em [setup/database.md](../setup/database.md) e [setup/development.md](../setup/development.md).

## 11. Segredos por ambiente — sem defaults versionados

**Decisão:** não há segredo padrão no código-fonte. `JWT_SECRET` é **obrigatória** (mínimo 32 caracteres) e a aplicação recusa iniciar sem ela (`util.TokenService`). O `run.sh` carrega um `.env` local (ignorado pelo Git) e, se `JWT_SECRET` não existir, gera uma e persiste nesse arquivo — desenvolvimento continua ergonômico, produção exige segredo explícito. As credenciais do MySQL (`MYSQL_USER`/`MYSQL_PASSWORD`) têm padrões de **desenvolvimento** (`taskflow`), mas devem ser sobrescritas em produção.

**Motivo:** impedir que um segredo default passe despercebido para produção. O `.env` ignorado pelo Git mantém segredos fora do repositório; valores por ambiente evitam compartilhar credenciais entre dev e prod.