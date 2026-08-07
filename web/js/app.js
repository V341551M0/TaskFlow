/*
    Lógica da web e comunicação com o backend
*/
const API_BASE_URL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : 'http://localhost:8080';
const STORAGE_KEY = 'taskflow-state';

document.addEventListener('DOMContentLoaded', function () {
  attachNavigation();
  attachCreateModal();
  loadItems();
});

function attachNavigation() {
  const botoes = document.querySelectorAll('.nav-buttons button');

  botoes.forEach(function (botao) {
    const texto = botao.textContent.trim();
    if (texto === 'Hábitos') {
      botao.addEventListener('click', function () {
        window.location.href = 'pages/HabitTask.html';
      });
    } else if (texto === 'Tarefas') {
      botao.addEventListener('click', function () {
        window.location.href = 'pages/Task.html';
      });
    } else if (texto === 'Tarefas Recorrentes') {
      botao.addEventListener('click', function () {
        window.location.href = 'pages/RecurringTask.html';
      });
    } else if (texto.includes('Voltar para Pagina Inicial') || texto.includes('Voltar para Página Inicial')) {
      botao.addEventListener('click', function () {
        window.location.href = '../index.html';
      });
    }
  });
}

function attachCreateModal() {
  const createButton = document.querySelector('.new-class-button button');
  const modal = document.getElementById('modal-task');
  const closeButtons = document.querySelectorAll('.close-btn, .cose-btn');
  const form = document.getElementById('form-task');

  if (createButton && modal) {
    createButton.addEventListener('click', () => {
      modal.style.display = 'flex';
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
        nome: document.getElementById('nome').value.trim(),
        data: document.getElementById('data').value,
        todosOsDias: document.getElementById('diario').checked,
        vezesAoDia: document.getElementById('vezes-dia').value
      };

      if (!dados.nome) {
        return;
      }

      const pageType = window.location.pathname.includes('HabitTask') ? 'habit' : window.location.pathname.includes('RecurringTask') ? 'recurring' : 'task';
      const endpoint = `${API_BASE_URL}/api/${pageType === 'habit' ? 'habits' : pageType === 'recurring' ? 'recurring-tasks' : 'tasks'}`;
      try {
        const resposta = await fetch(endpoint, {
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
          return;
        }
      } catch (error) {
        console.warn('API indisponível, usando armazenamento local.', error);
      }

      createLocalItem(pageType, dados);
      if (modal) {
        modal.style.display = 'none';
      }
      form.reset();
      loadItems();
    });
  }
}

async function loadItems() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/dashboard`);
    if (!response.ok) {
      throw new Error('Falha ao carregar dashboard');
    }

    const dashboard = await response.json();
    const tasks = dashboard.tasks || [];
    const habits = dashboard.habits || [];
    const recurringTasks = dashboard.recurringTasks || [];
    const heatmap = dashboard.heatmap || {};
    const allItems = [...tasks, ...habits, ...recurringTasks];

    persistLocalState({ tasks, habits, recurringTasks, heatmap });
    renderListsAndCharts(tasks, habits, recurringTasks, allItems, heatmap);
  } catch (error) {
    console.warn('Usando dados locais do navegador.', error);
    const localState = loadLocalState();
    const tasks = localState.tasks || [];
    const habits = localState.habits || [];
    const recurringTasks = localState.recurringTasks || [];
    const heatmap = localState.heatmap || {};
    const allItems = [...tasks, ...habits, ...recurringTasks];
    renderListsAndCharts(tasks, habits, recurringTasks, allItems, heatmap);
  }
}

function renderListsAndCharts(tasks, habits, recurringTasks, allItems, heatmap) {
  renderList('dashboard-list', allItems);
  renderList('task-list', tasks);
  renderList('habit-list', habits);
  renderList('recurring-list', recurringTasks);

  updateMetrics(tasks, habits, recurringTasks);
  renderDashboardCharts(tasks, habits, recurringTasks, allItems, heatmap);
  renderHeatmap(heatmap);
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
    <article class="item-card ${item.completedToday ? 'item-card-completed' : 'item-card-pending'}">
      <div>
        <strong>${item.name}</strong>
        <p>${item.date || 'Sem data'}</p>
      </div>
      <div class="item-actions">
        <span class="item-badge">${item.type || 'item'}</span>
        <div class="status-buttons">
          <button class="status-btn ${item.completedToday ? 'active' : ''}" data-action="complete" data-id="${item.id}" data-type="${item.type || 'item'}" data-date="${item.date || ''}">Concluído</button>
          <button class="status-btn ${!item.completedToday ? 'active' : ''}" data-action="pending" data-id="${item.id}" data-type="${item.type || 'item'}" data-date="${item.date || ''}">Não concluído</button>
        </div>
      </div>
    </article>
  `).join('');

  document.querySelectorAll('.status-btn').forEach((button) => {
    button.addEventListener('click', async (event) => {
      event.stopPropagation();
      const target = event.currentTarget;
      await setItemStatus(target.dataset.id, target.dataset.type, target.dataset.date, target.dataset.action);
    });
  });
}

async function setItemStatus(id, type, date, action) {
  const completed = action === 'complete';

  try {
    const resposta = await fetch(`${API_BASE_URL}/api/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, type, date, completed })
    });

    if (resposta.ok) {
      await loadItems();
      return;
    }
  } catch (error) {
    console.warn('API indisponível para marcação, atualizando localmente.', error);
  }

  const localState = loadLocalState();
  const targetList = type === 'habit' ? 'habits' : type === 'recurring' ? 'recurringTasks' : 'tasks';
  const list = localState[targetList] || [];
  const item = list.find((entry) => entry.id === id);
  if (item) {
    item.completedToday = completed;
    item.completionCount = item.completionCount || 0;
    const delta = Number(item.frequencyPerDay || 1);
    if (completed) {
      item.completionCount += delta;
    } else {
      item.completionCount = Math.max(0, item.completionCount - delta);
    }
  }
  persistLocalState(localState);
  await loadItems();
}

function updateMetrics(tasks, habits, recurringTasks) {
  const taskCount = document.getElementById('task-count');
  const habitCount = document.getElementById('habit-count');
  const recurringCount = document.getElementById('recurring-count');

  if (taskCount) taskCount.textContent = `${tasks.length}/-`;
  if (habitCount) habitCount.textContent = `${habits.length}/-`;
  if (recurringCount) recurringCount.textContent = `${recurringTasks.length}/-`;
}

function createLocalItem(type, dados) {
  const localState = loadLocalState();
  const item = {
    id: `local-${Date.now()}`,
    name: dados.nome,
    date: dados.data || new Date().toISOString().slice(0, 10),
    allDays: Boolean(dados.todosOsDias),
    frequencyPerDay: dados.vezesAoDia || '1',
    type,
    completedToday: false,
    completionCount: 0
  };

  const targetList = type === 'habit' ? 'habits' : type === 'recurring' ? 'recurringTasks' : 'tasks';
  localState[targetList] = localState[targetList] || [];
  localState[targetList].push(item);
  persistLocalState(localState);
}

function loadLocalState() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) {
      return { tasks: [], habits: [], recurringTasks: [], heatmap: {} };
    }
    return JSON.parse(stored);
  } catch (error) {
    console.warn('Não foi possível carregar o estado local.', error);
    return { tasks: [], habits: [], recurringTasks: [], heatmap: {} };
  }
}

function persistLocalState(state) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (error) {
    console.warn('Não foi possível persistir o estado local.', error);
  }
}

function renderDashboardCharts(tasks, habits, recurringTasks, allItems, heatmap) {
  const weeklyTotalElement = document.getElementById('weekly-total');
  const monthlyRateElement = document.getElementById('monthly-rate');
  const completionRatioElement = document.getElementById('completion-ratio');
  const summaryContainer = document.getElementById('activity-summary-container');

  if (weeklyTotalElement) {
    weeklyTotalElement.textContent = `${calculateWeeklyTotal(heatmap)} pts`;
  }

  if (monthlyRateElement) {
    const monthlyRate = calculateMonthlyRate(heatmap);
    monthlyRateElement.textContent = `${monthlyRate >= 0 ? '+' : ''}${monthlyRate}%`;
  }

  if (completionRatioElement) {
    const completed = allItems.filter((item) => item.completedToday).length;
    const total = allItems.length;
    const ratioLabel = total > 0 ? `${completed}/${total}` : '0/0';
    completionRatioElement.textContent = ratioLabel;
  }

  if (summaryContainer) {
    const summaryItems = allItems.slice(0, 5).map((item) => `
      <div class="summary-item">
        <span>${item.name}</span>
        <small>${item.completedToday ? 'Concluída' : 'Pendente'}</small>
      </div>
    `).join('');
    summaryContainer.innerHTML = summaryItems || '<p class="empty-state">Nenhuma atividade registrada ainda.</p>';
  }

  renderWeeklyBars(heatmap);
  renderMonthlyLine(heatmap);
  renderDonut(allItems);
}

function renderWeeklyBars(heatmap) {
  const bars = document.querySelectorAll('.bar-chart-svg .chart-bar');
  if (!bars.length) {
    return;
  }

  const values = [];
  const today = new Date();
  for (let index = 6; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    values.push(heatmap[formatIsoDate(date)] || 0);
  }

  const maxValue = Math.max(...values, 1);
  bars.forEach((bar, index) => {
    const value = values[index] || 0;
    const height = Math.max(8, (value / maxValue) * 90);
    const y = 120 - height;
    bar.setAttribute('y', y.toString());
    bar.setAttribute('height', height.toString());
  });
}

function renderMonthlyLine(heatmap) {
  const line = document.querySelector('.line-chart-svg .chart-line');
  if (!line) {
    return;
  }

  const values = [];
  const now = new Date();
  for (let index = 3; index >= 0; index -= 1) {
    const monthDate = new Date(now.getFullYear(), now.getMonth() - index, 1);
    values.push(sumValuesForMonth(heatmap, monthDate));
  }

  const maxValue = Math.max(...values, 1);
  const points = values.map((value, index) => {
    const x = 15 + index * 90;
    const y = 120 - (value / maxValue) * 90;
    return `${index === 0 ? 'M' : 'L'} ${x} ${y.toFixed(1)}`;
  }).join(' ');

  line.setAttribute('d', points);
}

function renderDonut(items) {
  const donutSegment = document.querySelector('.donut-segment');
  if (!donutSegment) {
    return;
  }

  const completed = items.filter((item) => item.completedToday).length;
  const total = items.length;
  const ratio = total > 0 ? completed / total : 0;
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  donutSegment.style.strokeDasharray = `${circumference}`;
  donutSegment.style.strokeDashoffset = `${circumference * (1 - ratio)}`;
}

function renderHeatmap(heatmap) {
  const heatmapGrid = document.querySelector('.heatmap-grid');
  if (!heatmapGrid) {
    return;
  }

  const daysToShow = 112;
  const cells = [];
  const today = new Date();

  for (let index = daysToShow - 1; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    const isoDate = formatIsoDate(date);
    const value = heatmap[isoDate] || 0;
    const level = getHeatLevel(value);
    cells.push(`<span class="day level-${level}" data-tooltip="${formatDateLabel(date)}: ${value} contribuições"></span>`);
  }

  const weeks = [];
  while (cells.length) {
    weeks.push(`<div class="week">${cells.splice(0, 7).join('')}</div>`);
  }

  heatmapGrid.innerHTML = weeks.join('');
}

function calculateWeeklyTotal(heatmap) {
  let total = 0;
  const today = new Date();
  for (let index = 6; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    total += Number(heatmap[formatIsoDate(date)] || 0);
  }
  return total;
}

function calculateMonthlyRate(heatmap) {
  const currentMonth = new Date();
  const previousMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1);
  const currentTotal = sumValuesForMonth(heatmap, currentMonth);
  const previousTotal = sumValuesForMonth(heatmap, previousMonth);

  if (previousTotal === 0) {
    return currentTotal > 0 ? 100 : 0;
  }

  return Math.round(((currentTotal - previousTotal) / previousTotal) * 100);
}

function sumValuesForMonth(heatmap, referenceDate) {
  let total = 0;
  const year = referenceDate.getFullYear();
  const month = referenceDate.getMonth();

  Object.entries(heatmap).forEach(([dateKey, value]) => {
    const [entryYear, entryMonth] = dateKey.split('-').map(Number);
    if (entryYear === year && entryMonth - 1 === month) {
      total += Number(value || 0);
    }
  });

  return total;
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
