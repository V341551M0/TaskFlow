/*
    Logica da web e comunicação com backend
*/
document.addEventListener("DOMContentLoaded", function () {
  // Seleciona todos os botões da navegação
  const botoes = document.querySelectorAll(".nav-buttons button");

  // Adiciona o evento de clique nos botões de navegação
  botoes.forEach(function (botao) {
    const texto = botao.textContent.trim();
    if (texto === "Hábitos") {
      botao.addEventListener("click", function () {
        // Redireciona para a página HabitTask.html na pasta pages
        window.location.href = "pages/HabitTask.html";
      });
    } else if (texto === "Tarefas") {
      botao.addEventListener("click", function () {
        window.location.href = "pages/Task.html";
      });
    } else if (texto === "Tarefas Recorrentes") {
      botao.addEventListener("click", function () {
        window.location.href = "pages/RecurringTask.html";
      });
    } else if (texto.includes("Voltar para Pagina Inicial") || texto.includes("Voltar para Página Inicial")) {
      botao.addEventListener("click", function () {
        window.location.href = "../index.html";
      });
    }
  });
});