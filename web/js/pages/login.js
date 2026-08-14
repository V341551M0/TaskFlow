const AUTH_KEY = 'taskflow-auth';

document.addEventListener('DOMContentLoaded', function () {
  const form = document.querySelector('.form-login form');
  if (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      localStorage.setItem(AUTH_KEY, 'true');
      window.location.href = '../index.html';7
    });
  }
});

let card = document.querySelector(".card");
let loginButton = document.querySelector(".loginButton");
let cadastroButton = document.querySelector(".cadastroButton");

loginButton.onclick = () => {
  card.classList.remove("cadastroActive")
  card.classList.add("loginActive")
}

cadastroButton.onclick = () => {
  card.classList.remove("loginActive")
  card.classList.add("cadastroActive")
}