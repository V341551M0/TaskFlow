/*
    Lógica da tela de login/cadastro e comunicação com o backend Java/MySQL
*/
const API_BASE_URL = 'http://localhost:8080';
const AUTH_KEY = 'taskflow-auth';
const USER_KEY = 'taskflow-user';
const HOME_URL = '../index.html';

document.addEventListener('DOMContentLoaded', function () {
  if (localStorage.getItem(AUTH_KEY) === 'true') {
    window.location.replace(HOME_URL);
    return;
  }

  const card = document.querySelector('.card');
  const loginForm = document.querySelector('.formLogin form');
  const registerForm = document.querySelector('.formCadastro form');
  const showLoginButton = document.querySelector('.fcLogin button');
  const showRegisterButton = document.querySelector('.fcCd button');

  if (showLoginButton && card) {
    showLoginButton.addEventListener('click', function () {
      card.classList.remove('cdActive');
      card.classList.add('loginActive');
    });
  }

  if (showRegisterButton && card) {
    showRegisterButton.addEventListener('click', function () {
      card.classList.remove('loginActive');
      card.classList.add('cdActive');
    });
  }

  if (loginForm) {
    loginForm.addEventListener('submit', function (event) {
      event.preventDefault();
      const username = loginForm.querySelector('input[type="text"]').value.trim();
      const password = loginForm.querySelector('input[type="password"]').value;
      authenticate('/api/auth/login', { username, password }, 'E-mail, nome de usuário ou senha inválidos.');
    });
  }

  if (registerForm) {
    registerForm.addEventListener('submit', function (event) {
      event.preventDefault();
      const inputs = registerForm.querySelectorAll('input');
      const name = inputs[0].value.trim();
      const email = inputs[1].value.trim();
      const password = inputs[2].value;
      const confirmPassword = inputs[3].value;

      if (password !== confirmPassword) {
        alert('As senhas não coincidem.');
        return;
      }

      authenticate('/api/auth/register', { name, email, password }, 'Não foi possível criar a conta.');
    });
  }
});

async function authenticate(endpoint, payload, fallbackMessage) {
  const values = Object.values(payload);
  if (values.some(function (value) {
    return typeof value !== 'string' || value.trim() === '';
  })) {
    alert('Preencha todos os campos.');
    return;
  }

  const submitButton = findActiveSubmitButton();
  if (submitButton) {
    submitButton.disabled = true;
  }

  try {
    const response = await fetch(API_BASE_URL + endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));

    if (response.ok) {
      localStorage.setItem(AUTH_KEY, 'true');
      localStorage.setItem(USER_KEY, JSON.stringify({
        id: data.id,
        username: data.username,
        email: data.email
      }));
      window.location.href = HOME_URL;
      return;
    }

    alert(data.message || fallbackMessage);
  } catch (error) {
    console.warn('Não foi possível conectar ao servidor backend Java:', error);
    alert('Erro de comunicação com o servidor. Verifique se a API Java/MySQL está rodando.');
  } finally {
    if (submitButton) {
      submitButton.disabled = false;
    }
  }
}

function findActiveSubmitButton() {
  const card = document.querySelector('.card');
  if (!card) {
    return null;
  }
  const form = card.classList.contains('cdActive')
    ? document.querySelector('.formCadastro form')
    : document.querySelector('.formLogin form');
  return form ? form.querySelector('button[type="submit"]') : null;
}
