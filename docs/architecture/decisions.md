# Decisões Arquiteturais

Este documento registra as principais decisões de projeto e o motivo de cada uma. Serve como referência para entender *por que* o sistema é estruturado assim.

## 1. Arquitetura em camadas

**Decisão:** separar o código em Controller → Service → Repository → DTO.

**Motivo:** isolar responsabilidades (apresentação, regras de negócio e persistência). Isso torna o código testável (cada camada pode ser testada isoladamente), facilita a manutenção e demonstra boas práticas em um projeto de portfólio.

## 2. Servidor HTTP embutido do JDK (sem Spring Boot)

**Decisão:** usar `com.sun.net.httpserver.HttpServer`, que já vem no JDK.

**Motivo:** manter o projeto leve e com dependências mínimas (apenas o driver MySQL e o JUnit). A troca por um framework (Spring Boot, por exemplo) não exige reescrever as camadas internas — apenas a camada Controller — o que reforça o benefício da separação em camadas.

## 3. Frontend estático sem framework

**Decisão:** HTML, CSS e JavaScript puros servidos como arquivos estáticos.

**Motivo:** escopo e simplicidade. O frontend consome a API via `fetch` com CORS liberado, sem etapa de build. Uma evolução possível seria adicionar um bundler ou framework SPA.

## 4. Schema executado automaticamente no startup

**Decisão:** `util.DatabaseConnection.initialize()` lê `db/schema.sql` e executa os `CREATE TABLE IF NOT EXISTS` sempre que a aplicação inicia.

**Motivo:** facilita o setup em novos ambientes — quem clona o projeto e conecta ao MySQL já tem o schema criado sem passo manual. As instruções de preparação do banco estão em [setup/database.md](../setup/database.md).

## 5. Senhas com hash (SHA-256 + salt)

**Decisão:** `util.PasswordUtil` gera um salt aleatório por usuário e armazena `salt:hash` na coluna `senha`. A verificação usa comparação em tempo constante.

**Motivo:** nunca armazenar senha em texto puro. A serialização JSON do `UserDto` nunca expõe o hash.

## 6. CORS liberado

**Decisão:** todos os endpoints respondem com `Access-Control-Allow-Origin: *`.

**Motivo:** ambiente de desenvolvimento, em que frontend (ex.: Live Server na porta 5501) e API (porta 8080) rodam em origens diferentes. Para produção, esse cabeçalho deve ser restrito ao domínio do frontend.

## 7. Autenticação simples via localStorage

**Decisão:** após login/cadastro, o frontend guarda `taskflow-auth` (e o usuário em `taskflow-user`) no `localStorage` e redireciona para a tela inicial. As páginas internas verificam essa flag.

**Motivo:** controle de acesso adequado ao escopo do projeto. O backend autentica de verdade contra o banco (`/api/auth/login` e `/api/auth/register`); a persistência da sessão no cliente é propositalmente simples. Uma evolução seria usar tokens (JWT) e validar em todas as rotas.

## 8. Item genérico com três tabelas e heatmap desnormalizado

**Decisão:** tarefas, hábitos e tarefas recorrentes vivem em tabelas separadas porém com a mesma estrutura; o histórico de conclusões é normalizado em `item_history` e agregado em `daily_heatmap`.

**Motivo:** simplicidade de consulta por tipo e leitura rápida do heatmap (sem agregação a cada request). O custo é a duplicação estrutural entre as três tabelas — aceitável para o escopo atual.

## 9. SQL com `PreparedStatement`

**Decisão:** todas as consultas usam parâmetros vinculados.

**Motivo:** prevenção de SQL Injection e melhor legibilidade em relação à concatenação de strings.

## 10. Script de inicialização (`run.sh`)

**Decisão:** um único script sobe a infraestrutura local de forma idempotente, detectando o MySQL a usar: primeiro tenta o **MySQL do sistema** (`127.0.0.1:3306`, persistente) e, sem acesso, cai para uma **instância isolada** em `/tmp/taskflow-mysql` na porta `3307`.

**Motivo:** o MySQL do sistema usa `auth_socket` e o AppArmor do Ubuntu restringe o `mysqld` a `/tmp` e `/var/lib/mysql`. Preferir o banco do sistema quando acessível mantém os dados persistentes entre reinicializações; o fallback em `/tmp` elimina passos manuais em máquinas sem essa configuração. Detalhes em [setup/database.md](../setup/database.md) e [setup/development.md](../setup/development.md).