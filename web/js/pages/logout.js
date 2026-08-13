const AUTH_KEY = 'taskflow-auth';

document.addEventListener('DOMContentLoaded', function () {
  const form = document.querySelector('.form-login form');
  if (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      localStorage.setItem(AUTH_KEY, 'true');
      window.location.href = '../index.html';
    });
  }
});