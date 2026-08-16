# Visão Geral da Arquitetura

O TaskFlow é uma aplicação web de acompanhamento de tarefas, hábitos e tarefas recorrentes. O backend é escrito em Java puro (sem framework) e expõe uma API REST consumida por um frontend estático (HTML, CSS e JavaScript).

## Fluxo de uma requisição

```
                    Frontend (web/)
                    HTML + CSS + JS
                         │
                         │  HTTP / JSON (fetch)
                         ▼
              ┌───────────────────────────┐
              │        Controller         │  camada HTTP
              │  TaskController           │  (com.sun.net.httpserver)
              │  AuthController           │
              └────────────┬──────────────┘
                           ▼
              ┌───────────────────────────┐
              │         Service           │  regras de negócio
              │  TaskService              │
              │  UserService              │
              └────────────┬──────────────┘
                           ▼
              ┌───────────────────────────┐
              │        Repository         │  persistência (JDBC)
              │  TaskRepository           │
              │  UserRepository           │
              └────────────┬──────────────┘
                           ▼
              ┌───────────────────────────┐
              │          MySQL            │
              └───────────────────────────┘
```

## Componentes do backend

| Componente | Responsabilidade |
|---|---|
| `app.TaskApplication` | Ponto de entrada. Cria o servidor HTTP, registra as rotas e inicia a API. |
| `controller.*` | Recebem requisições HTTP, fazem parse/serialização de JSON e devolvem códigos de status. |
| `service.*` | Regras de negócio, validações e orquestração entre controller e repository. |
| `repository.*` | Acesso ao banco com `PreparedStatement` (protege contra SQL Injection). |
| `dto.*` | Objetos de transporte de dados entre camadas. |
| `util.DatabaseConnection` | Conexão JDBC com o MySQL, configurável por variáveis de ambiente. |
| `util.Json` | Serialização/parse de JSON (o projeto não usa biblioteca externa de JSON). |
| `util.PasswordUtil` | Geração e verificação de hash de senha (SHA-256 com salt). |

## Componentes do frontend

| Página | Finalidade |
|---|---|
| `web/index.html` | Dashboard com gráficos, heatmap e resumo das atividades. |
| `web/pages/login.html` | Tela de login/cadastro. |
| `web/pages/Task.html` | Gestão de tarefas. |
| `web/pages/HabitTask.html` | Gestão de hábitos. |
| `web/pages/RecurringTask.html` | Gestão de tarefas recorrentes. |

O frontend conversa com a API através de `fetch` em `web/js/app.js` e `web/js/pages/login.js`.

## Camadas de responsabilidade em detalhe

Veja [layers.md](layers.md) para a responsabilidade individual de cada camada e [decisions.md](decisions.md) para as decisões arquiteturais que justificam esta estrutura.