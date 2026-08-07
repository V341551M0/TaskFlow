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
  const closeButtons = document.querySelectorAll('.close-btn, .cose-btn');
  const form = document.getElementById('form-task');

  if (createButton) {
    createButton.addEventListener('click', () => {
      if (modal) {
        modal.style.display = 'flex';
      }
    });
  }

  closeButtons.forEach((button) => {
    button.addEventListener('click', () => {
      if (modal) {
        modal.style.display = 'none';
      }
    });
  });

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
        if (modal) {
          modal.style.display = 'none';
        }
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
    const allItems = [...tasks, ...habits, ...recurringTasks];

    renderList('dashboard-list', allItems);
    renderList('task-list', tasks);
    renderList('habit-list', habits);
    renderList('recurring-list', recurringTasks);

    updateMetrics(tasks, habits, recurringTasks);
    renderHeatmap(allItems);
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
      <div class="item-actions">
        <span class="item-badge">${item.type || 'item'}</span>
        <label class="completion-toggle">
          <input type="checkbox" class="completion-checkbox" data-id="${item.id}" data-type="${item.type || 'item'}" data-date="${item.date || ''}" ${item.completedToday ? 'checked' : ''}>
          <span>Marcar</span>
        </label>
      </div>
    </article>
  `).join('');

  document.querySelectorAll('.completion-checkbox').forEach((checkbox) => {
    checkbox.addEventListener('change', async (event) => {
      event.stopPropagation();
      const target = event.currentTarget;
      await markItemAsCompleted(target.dataset.id, target.dataset.type, target.dataset.date);
    });
  });
}

async function markItemAsCompleted(id, type, date) {
  try {
    const resposta = await fetch(`${API_BASE_URL}/api/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, type, date })
    });

    if (resposta.ok) {
      await loadItems();
    }
  } catch (error) {
    console.error('Erro ao atualizar atividade:', error);
  }
}

function updateMetrics(tasks, habits, recurringTasks) {
  const taskCount = document.getElementById('task-count');
  const habitCount = document.getElementById('habit-count');
  const recurringCount = document.getElementById('recurring-count');

  if (taskCount) taskCount.textContent = `${tasks.length}/-`;
  if (habitCount) habitCount.textContent = `${habits.length}/-`;
  if (recurringCount) recurringCount.textContent = `${recurringTasks.length}/-`;
}

function renderHeatmap(items) {
  const heatmapGrid = document.querySelector('.heatmap-grid');
  if (!heatmapGrid) {
    return;
  }

  const heatmapData = buildHeatmapData(items);
  const daysToShow = 112;
  const cells = [];
  const today = new Date();

  for (let index = daysToShow - 1; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    const isoDate = formatIsoDate(date);
    const value = heatmapData[isoDate] || 0;
    const level = getHeatLevel(value);
    cells.push(`<span class="day level-${level}" data-tooltip="${formatDateLabel(date)}: ${value} contribuições"></span>`);
  }

  const weeks = [];
  while (cells.length) {
    weeks.push(`<div class="week">${cells.splice(0, 7).join('')}</div>`);
  }

  heatmapGrid.innerHTML = weeks.join('');
}

function buildHeatmapData(items) {
  const heatmapData = {};

  items.forEach((item) => {
    const date = item.date;
    if (!date) {
      return;
    }

    const contribution = item.completedToday ? Number(item.frequencyPerDay || 1) : 0;
    if (contribution > 0) {
      heatmapData[date] = (heatmapData[date] || 0) + contribution;
    }
  });

  return heatmapData;
}

function getHeatLevel(value) {
  if (value <= 0) {
    return 0;
  }
  if (value <= 1) {
    return 1;
  }
  if (value <= 2) {
    return 2;
  }
  if (value <= 4) {
    return 3;
  }
  return 4;
}

function formatIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateLabel(date) {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(date);
}
