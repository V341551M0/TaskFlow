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

// Abrir o modal
document.querySelector('.new-class-button button').addEventListener('click', () => {
    document.getElementById('modal-task').style.display = 'flex';
});

// Fechar o modal ao clicar no botão 'x'
document.querySelector('.close-btn').addEventListener('click', () => {
    document.getElementById('modal-task').style.display = 'none';
});

// Enviar os dados para o servidor Java
document.getElementById('form-task').addEventListener('submit', async (event) => {
    event.preventDefault(); // Impede recarregar a página

    const dados = {
        nome: document.getElementById('nome').value,
        data: document.getElementById('data').value,
        todosOsDias: document.getElementById('diario').checked,
        vezesAoDia: document.getElementById('vezes-dia').value
    };

    // Envia os dados para a API Java
    const resposta = await fetch('/api/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(dados)
    });

    if (resposta.ok) {
        alert('Criado com sucesso!');
        document.getElementById('modal-task').style.display = 'none';
    }
});