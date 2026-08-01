/*
    Logica da web e comunicação com backend
*/
document.addEventListener("DOMContentLoaded", function () {
  // Seleciona todos os botões da navegação
  const botoes = document.querySelectorAll(".nav-buttons button");

  // Percorre os botões até encontrar o botão "Hábitos"
  botoes.forEach(function (botao) {
    if (botao.textContent.trim() === "Hábitos") {
      botao.addEventListener("click", function () {
        // Redireciona para a página HabitTask.html na pasta pages
        window.location.href = "pages/HabitTask.html";
      });
    }
  });
});