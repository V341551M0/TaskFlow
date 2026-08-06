/*
    Logica da web e comunicação com backend
*/
const API_BASE_URL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : 'http://localhost:8080';

document.addEventListener("DOMContentLoaded", function () {
  const botoes = document.querySelectorAll(".nav-buttons button");

  botoes.forEach(function (botao) {
    const texto = botao.textContent.trim();
    if (texto === "Hábitos") {
      botao.addEventListener("click", function () {
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

  const createButton = document.querySelector('.new-class-button button');
  const modal = document.getElementById('modal-task');
  const closeButton = document.querySelector('.close-btn');
  const form = document.getElementById('form-task');

  if (createButton) {
    createButton.addEventListener('click', () => {
      modal.style.display = 'flex';
    });
  }

  if (closeButton) {
    closeButton.addEventListener('click', () => {
      modal.style.display = 'none';
    });
  }

  if (form) {
    form.addEventListener('submit', async (event) => {
      event.preventDefault();

      const dados = {
        nome: document.getElementById('nome').value,
        data: document.getElementById('data').value,
        todosOsDias: document.getElementById('diario').checked,
        vezesAoDia: document.getElementById('vezes-dia').value
      };

      const pageType = window.location.pathname.includes('HabitTask') ? 'habit' : window.location.pathname.includes('RecurringTask') ? 'recurring' : 'task';
      const resposta = await fetch(`${API_BASE_URL}/api/${pageType === 'habit' ? 'habits' : pageType === 'recurring' ? 'recurring-tasks' : 'tasks'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(dados)
      });

      if (resposta.ok) {
        modal.style.display = 'none';
        form.reset();
        loadItems();
      }
    });
  }

  loadItems();
});

async function loadItems() {
  try {
    const [tasksResponse, habitsResponse, recurringResponse] = await Promise.all([
      fetch(`${API_BASE_URL}/api/tasks`),
      fetch(`${API_BASE_URL}/api/habits`),
      fetch(`${API_BASE_URL}/api/recurring-tasks`)
    ]);

    const tasks = await tasksResponse.json();
    const habits = await habitsResponse.json();
    const recurringTasks = await recurringResponse.json();

    renderList('dashboard-list', [...tasks, ...habits, ...recurringTasks]);
    renderList('task-list', tasks);
    renderList('habit-list', habits);
    renderList('recurring-list', recurringTasks);

    updateMetrics(tasks, habits, recurringTasks);
  } catch (error) {
    console.error('Erro ao carregar itens:', error);
  }
}

function renderList(containerId, items) {
  const container = document.getElementById(containerId);
  if (!container) {
    return;
  }

  if (!items || items.length === 0) {
    container.innerHTML = '<p class="empty-state">Nenhum item cadastrado ainda.</p>';
    return;
  }

  container.innerHTML = items.map(item => `
    <article class="item-card">
      <div>
        <strong>${item.name}</strong>
        <p>${item.date || 'Sem data'}</p>
      </div>
      <span class="item-badge">${item.type || 'item'}</span>
    </article>
  `).join('');
}

function updateMetrics(tasks, habits, recurringTasks) {
  const taskCount = document.getElementById('task-count');
  const habitCount = document.getElementById('habit-count');
  const recurringCount = document.getElementById('recurring-count');

  if (taskCount) taskCount.textContent = `${tasks.length}/-`;
  if (habitCount) habitCount.textContent = `${habits.length}/-`;
  if (recurringCount) recurringCount.textContent = `${recurringTasks.length}/-`;
}
