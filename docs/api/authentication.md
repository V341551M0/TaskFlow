# Autenticação

Endpoints de login e cadastro. Ambas as rotas aceitam **apenas POST** e recebem corpo em JSON.

> Base URL: `http://localhost:8080` (configurável pela aplicação).
> Todas as respostas incluem os cabeçalhos CORS necessários para o frontend.

---

## POST /api/auth/register

Cria uma nova conta. O **e-mail** é o identificador de login (único); o **nome de usuário** também pode ser usado no login (único).

### Request

Headers:

```
Content-Type: application/json
```

Body:

```json
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "minha-senha"
}
```

### Response

**201 Created**

```json
{
  "id": "49d4663b",
  "username": "Maria Silva",
  "email": "maria@email.com",
  "date": "2026-08-16"
}
```

> A senha nunca é retornada. Internamente ela é armazenada como `salt:hash` (SHA-256 com salt).

### Erros

| Código | Situação | Exemplo de body |
|---|---|---|
| `400` | Campo obrigatório vazio | `{ "message": "Informe um nome de usuário." }`, `{ "message": "Informe um e-mail." }`, `{ "message": "Informe uma senha." }` |
| `409` | E-mail já cadastrado | `{ "message": "Já existe uma conta com este e-mail." }` |
| `409` | Nome de usuário já em uso | `{ "message": "Este nome de usuário já está em uso." }` |
| `405` | Método diferente de POST | `{ "message": "Method not allowed" }` |

---

## POST /api/auth/login

Autentica um usuário existente. O campo `username` aceita **e-mail ou nome de usuário**.

### Request

Headers:

```
Content-Type: application/json
```

Body:

```json
{
  "username": "maria@email.com",
  "password": "minha-senha"
}
```

```json
{
  "username": "Maria Silva",
  "password": "minha-senha"
}
```

### Response

**200 OK**

```json
{
  "id": "49d4663b",
  "username": "Maria Silva",
  "email": "maria@email.com",
  "date": "2026-08-16"
}
```

### Erros

| Código | Situação | Exemplo de body |
|---|---|---|
| `401` | Credenciais inválidas ou usuário inexistente | `{ "message": "E-mail, nome de usuário ou senha inválidos." }` |
| `405` | Método diferente de POST | `{ "message": "Method not allowed" }` |

---

## Como o frontend usa

`web/js/pages/login.js`:

1. **Cadastro:** envia `POST /api/auth/register` com `{ name, email, password }`.
2. **Login:** envia `POST /api/auth/login` com `{ username, password }` — `username` pode ser e-mail ou nome de usuário.
3. Em caso de sucesso, grava no `localStorage`:
   - `taskflow-auth = "true"`
   - `taskflow-user` = `{ id, username, email }`
4. Redireciona para a tela inicial (`index.html`).

As páginas internas verificam a flag `taskflow-auth` (em `web/js/app.js`) e redirecionam para o login quando ausente.