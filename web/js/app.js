/*
    Lógica da web e comunicação com o backend
*/
// URL da API definida em web/config.js (window.TASKFLOW_API_URL) — por ambiente.
const API_BASE_URL = window.TASKFLOW_API_URL || '';
const STORAGE_KEY = 'taskflow-state';
const AUTH_KEY = 'taskflow-auth';
const TOKEN_KEY = 'taskflow-token';

/**
 * Cliente HTTP centralizado: prefixa a URL da API, anexa o token JWT e
 * trata 401 (encerra a sessão e redireciona para o login).
 */
async function apiFetch(path, options) {
  options = options || {};
  const headers = Object.assign({}, options.headers || {}, getAuthHeaders());
  const response = await fetch(API_BASE_URL + path, Object.assign({}, options, { headers }));
  if (handleUnauthorized(response)) {
    return null;
  }
  return response;
}

document.addEventListener('DOMContentLoaded', function () {
  if (!requireAuth()) {
    return;
  }
  attachNavigation();
  attachCreateModal();
  attachLogout();
  loadItems();
});

function requireAuth() {
  if (localStorage.getItem(AUTH_KEY) === 'true' && localStorage.getItem(TOKEN_KEY)) {
    return true;
  }
  window.location.replace(getLoginUrl());
  return false;
}

function getAuthHeaders() {
  const token = localStorage.getItem(TOKEN_KEY) || '';
  return {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  };
}

function handleUnauthorized(response) {
  if (response && response.status === 401) {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(TOKEN_KEY);
    window.location.replace(getLoginUrl());
    return true;
  }
  return false;
}

function attachLogout() {
  document.querySelectorAll('.logout-btn').forEach(function (button) {
    button.addEventListener('click', function (event) {
      event.preventDefault();
      localStorage.removeItem(AUTH_KEY);
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = getLoginUrl();
    });
  });
}

function getLoginUrl() {
  const isSubpage = window.location.pathname.includes('/pages/') ||
                    window.location.pathname.endsWith('Task.html') ||
                    window.location.pathname.endsWith('HabitTask.html') ||
                    window.location.pathname.endsWith('RecurringTask.html');
  return isSubpage ? 'login.html' : 'pages/login.html';
}

window.openTaskModal = function () {
  const modal = document.getElementById('modal-task');
  if (modal) {
    modal.style.display = 'flex';
  }
};

window.closeTaskModal = function () {
  const modal = document.getElementById('modal-task');
  if (modal) {
    modal.style.display = 'none';
  }
};

function attachNavigation() {
  const elements = document.querySelectorAll('.nav-buttons button, .nav-buttons a');
  const isSubpage = window.location.pathname.includes('/pages/') || 
                    window.location.pathname.endsWith('Task.html') || 
                    window.location.pathname.endsWith('HabitTask.html') || 
                    window.location.pathname.endsWith('RecurringTask.html');
  const pagesPrefix = isSubpage ? '' : 'pages/';
  const homePrefix = isSubpage ? '../' : './';

  elements.forEach(function (element) {
    element.addEventListener('click', function (event) {
      const texto = element.textContent.trim().toLowerCase();
      let targetUrl = null;

      // Não interceptar botões de criação de atividades (ex: "Criar Novo Hábito")
      if (texto.startsWith('criar')) {
        return;
      }

      if (texto.includes('voltar') || texto.includes('pagina inicial') || texto.includes('página inicial')) {
        targetUrl = homePrefix + 'index.html';
      } else if (texto === 'tarefas recorrentes') {
        targetUrl = pagesPrefix + 'RecurringTask.html';
      } else if (texto === 'hábitos' || texto === 'habitos') {
        targetUrl = pagesPrefix + 'HabitTask.html';
      } else if (texto === 'tarefas') {
        targetUrl = pagesPrefix + 'Task.html';
      }

      if (targetUrl) {
        event.preventDefault();
        window.location.href = targetUrl;
      }
    });
  });
}

function attachCreateModal() {
  const createButtons = document.querySelectorAll('#btn-nova-tarefa, #btn-novo-habito, #btn-nova-recorrente, .new-class-button button, .new-class-button a, .add-task-btn, [data-action="open-modal"]');
  const modal = document.getElementById('modal-task');
  const closeButtons = document.querySelectorAll('.close-btn, .cose-btn, .modal-close');
  const form = document.getElementById('form-task');

  createButtons.forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      openTaskModal();
    });
  });

  closeButtons.forEach((button) => {
    button.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      closeTaskModal();
    });
  });

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        closeTaskModal();
      }
    });
  }

  if (form) {
    form.addEventListener('submit', async (event) => {
      event.preventDefault();

      const nomeInput = document.getElementById('nome');
      const dataInput = document.getElementById('data');
      const diarioInput = document.getElementById('diario');
      const vezesDiaInput = document.getElementById('vezes-dia');

      const nome = nomeInput ? nomeInput.value.trim() : '';
      const dataVal = dataInput ? dataInput.value : '';
      const diario = diarioInput ? diarioInput.checked : false;
      const vezesDia = vezesDiaInput ? vezesDiaInput.value : '1';

      if (!nome) {
        alert('Por favor, preencha o nome da atividade.');
        return;
      }

      const dados = {
        nome: nome,
        data: dataVal,
        todosOsDias: String(diario),
        vezesAoDia: String(vezesDia)
      };

      const path = window.location.pathname;
      const pageType = path.includes('HabitTask') ? 'habit' : path.includes('RecurringTask') ? 'recurring' : 'task';

      try {
        const resposta = await apiFetch(`/api/${pageType === 'habit' ? 'habits' : pageType === 'recurring' ? 'recurring-tasks' : 'tasks'}`, {
          method: 'POST',
          body: JSON.stringify(dados)
        });

        if (!resposta) {
          return;
        }

        if (resposta.ok) {
          closeTaskModal();
          form.reset();
          await loadItems();
          return;
        } else {
          const err = await resposta.json().catch(() => ({}));
          alert(err.message || 'Erro ao criar atividade no backend Java MySQL.');
        }
      } catch (error) {
        console.warn('Não foi possível conectar ao servidor backend Java:', error);
        alert('Erro de comunicação com o servidor backend Java MySQL.');
      }
    });
  }
}

async function loadItems() {
  showLoading(true);
  try {
    const response = await apiFetch('/api/dashboard');
    if (!response) {
      return;
    }
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
  } finally {
    showLoading(false);
  }
}

function showLoading(active) {
  let overlay = document.getElementById('tf-loading');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.id = 'tf-loading';
    overlay.textContent = 'Carregando...';
    overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;display:none;' +
      'align-items:center;justify-content:center;background:rgba(255,255,255,0.75);' +
      'z-index:9999;font-family:sans-serif;font-size:1.1em;color:#333;';
    document.body.appendChild(overlay);
  }
  overlay.style.display = active ? 'flex' : 'none';
  document.body.classList.toggle('loading', active);
}

function renderListsAndCharts(tasks, habits, recurringTasks, allItems, heatmap) {
  renderList('dashboard-list', allItems);
  renderList('task-list', tasks);
  renderList('habit-list', habits);
  renderList('recurring-list', recurringTasks);

  updateMetrics(tasks, habits, recurringTasks);
  renderDashboardCharts(tasks, habits, recurringTasks, allItems, heatmap);

  if (typeof window.refreshHeatmap === 'function') {
    window.refreshHeatmap(heatmap);
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

  container.innerHTML = items.map(item => {
    const status = item.status || (item.completedToday ? 'completed' : 'pending');
    const isCompleted = status === 'completed';
    const isFailed = status === 'failed';
    const isFinalized = isCompleted || isFailed;
    return `
      <article class="item-card ${isCompleted ? 'item-card-completed' : isFailed ? 'item-card-failed' : 'item-card-pending'}" data-item-id="${item.id}" data-item-type="${item.type || 'item'}">
        <div>
          <strong>${item.name}</strong>
          <p>${item.date || 'Sem data'}</p>
        </div>
        <div class="item-actions">
          <span class="item-badge">${item.type || 'item'}</span>
          <div class="status-buttons">
            <button class="status-btn ${isCompleted ? 'active' : ''}" ${isFinalized ? 'disabled title="Status finalizado"' : ''} data-action="complete" data-id="${item.id}" data-type="${item.type || 'item'}" data-date="${item.date || ''}">${isCompleted ? 'Concluído' : 'Concluir'}</button>
            <button class="status-btn ${isFailed ? 'active failed' : ''}" ${isFinalized ? 'disabled title="Status finalizado"' : ''} data-action="failed" data-id="${item.id}" data-type="${item.type || 'item'}" data-date="${item.date || ''}">${isFailed ? 'Falhou' : 'Falha'}</button>
            <button class="status-btn delete-btn" data-action="delete" data-id="${item.id}" data-type="${item.type || 'item'}">Apagar</button>
          </div>
        </div>
      </article>
    `;
  }).join('');

  document.querySelectorAll('.status-btn').forEach((button) => {
    if (button.dataset.action === 'delete') {
      button.addEventListener('click', async (event) => {
        event.stopPropagation();
        const target = event.currentTarget;
        const confirmed = window.confirm('Deseja realmente apagar esta atividade?');
        if (!confirmed) {
          return;
        }
        await deleteItem(target.dataset.id, target.dataset.type);
      });
      return;
    }
    button.addEventListener('click', async (event) => {
      event.stopPropagation();
      const target = event.currentTarget;
      if (target.disabled) {
        alert('Atividade já possui status finalizado (concluída ou falha) e não pode ser alterada novamente.');
        return;
      }
      await setItemStatus(target.dataset.id, target.dataset.type, target.dataset.date, target.dataset.action);
    });
  });
}

async function deleteItem(id, type) {
  try {
    const resposta = await apiFetch('/api/delete', {
      method: 'POST',
      body: JSON.stringify({ id, type })
    });

    if (!resposta) {
      return;
    }

    if (resposta.ok) {
      await loadItems();
      return;
    }
  } catch (error) {
    console.warn('API indisponível para exclusão, removendo localmente.', error);
  }

  const localState = loadLocalState();
  const targetList = type === 'habit' ? 'habits' : type === 'recurring' ? 'recurringTasks' : 'tasks';
  const list = localState[targetList] || [];
  const item = list.find((entry) => entry.id === id);
  const updatedList = list.filter((entry) => entry.id !== id);
  localState[targetList] = updatedList;

  if (item) {
    localState.heatmap = localState.heatmap || {};
    if (item.history && Object.keys(item.history).length > 0) {
      Object.entries(item.history).forEach(([date, contribution]) => {
        const current = Number(localState.heatmap[date] || 0);
        const next = current - Number(contribution);
        if (next <= 0) {
          delete localState.heatmap[date];
        } else {
          localState.heatmap[date] = next;
        }
      });
    } else {
      const completionDate = item.date || new Date().toISOString().slice(0, 10);
      const delta = Number(item.frequencyPerDay || 1);
      const current = Number(localState.heatmap[completionDate] || 0);
      if (item.status === 'completed' || item.completedToday) {
        const next = current - delta;
        if (next <= 0) delete localState.heatmap[completionDate];
        else localState.heatmap[completionDate] = next;
      } else if (item.status === 'failed') {
        const next = current + delta;
        if (next <= 0) delete localState.heatmap[completionDate];
        else localState.heatmap[completionDate] = next;
      }
    }
  }

  persistLocalState(localState);
  await loadItems();
}

async function setItemStatus(id, type, date, action) {
  const status = action === 'complete' ? 'completed' : action === 'failed' ? 'failed' : 'pending';

  try {
    const resposta = await apiFetch('/api/complete', {
      method: 'POST',
      body: JSON.stringify({ id, type, date, status })
    });

    if (!resposta) {
      return;
    }

    if (resposta.ok) {
      await loadItems();
      return;
    } else {
      const errData = await resposta.json().catch(() => ({}));
      if (errData.message) {
        alert(errData.message);
        return;
      }
    }
  } catch (error) {
    console.warn('API indisponível para marcação, atualizando localmente.', error);
  }

  const localState = loadLocalState();
  const targetList = type === 'habit' ? 'habits' : type === 'recurring' ? 'recurringTasks' : 'tasks';
  const list = localState[targetList] || [];
  const item = list.find((entry) => entry.id === id);
  if (item) {
    const previousStatus = item.status || (item.completedToday ? 'completed' : 'pending');
    if (previousStatus === 'completed' || previousStatus === 'failed') {
      alert('Atividade já possui status finalizado (concluída ou falha) e não pode ser alterada novamente.');
      return;
    }
    const delta = Number(item.frequencyPerDay || 1);
    const completionDate = date || item.date || new Date().toISOString().slice(0, 10);
    const normalizedState = status === 'completed' ? 'completed' : status === 'failed' ? 'failed' : 'pending';

    item.history = item.history || {};

    item.status = normalizedState;
    item.completedToday = normalizedState === 'completed';
    item.completionCount = item.completionCount || 0;
    if (normalizedState === 'completed') {
      item.completionCount += delta;
      item.history[completionDate] = delta;
    } else if (normalizedState === 'failed') {
      item.completionCount = Math.max(0, item.completionCount - delta);
      item.history[completionDate] = -delta;
    } else {
      item.completionCount = Math.max(0, item.completionCount - delta);
      delete item.history[completionDate];
    }

    localState.heatmap = localState.heatmap || {};
    const currentValue = Number(localState.heatmap[completionDate] || 0);
    let nextValue = currentValue;
    if (normalizedState === 'completed') {
      nextValue = currentValue + delta;
    } else if (normalizedState === 'failed') {
      nextValue = currentValue - delta;
    } else if (previousStatus === 'completed') {
      nextValue = currentValue - delta;
    } else if (previousStatus === 'failed') {
      nextValue = currentValue + delta;
    }
    if (nextValue <= 0) {
      delete localState.heatmap[completionDate];
    } else {
      localState.heatmap[completionDate] = nextValue;
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
    const completed = allItems.filter((item) => (item.status || (item.completedToday ? 'completed' : 'pending')) === 'completed').length;
    const total = allItems.length;
    const ratioLabel = total > 0 ? `${completed}/${total}` : '0/0';
    completionRatioElement.textContent = ratioLabel;
  }

  if (summaryContainer) {
    const summaryItems = allItems.slice(0, 5).map((item) => {
      const status = item.status || (item.completedToday ? 'completed' : 'pending');
      const label = status === 'completed' ? 'Concluída' : status === 'failed' ? 'Falha' : 'Pendente';
      return `
        <div class="summary-item">
          <span>${item.name}</span>
          <small>${label}</small>
        </div>
      `;
    }).join('');
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

  const labels = document.querySelectorAll('.chart-labels span');
  const values = [];
  const today = new Date();
  for (let index = 6; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    values.push(heatmap[formatIsoDate(date)] || 0);

    if (labels[index]) {
      const label = date.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '');
      labels[index].textContent = label.charAt(0).toUpperCase() + label.slice(1);
    }
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

  const completed = items.filter((item) => (item.status || (item.completedToday ? 'completed' : 'pending')) === 'completed').length;
  const total = items.length;
  const ratio = total > 0 ? completed / total : 0;
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  donutSegment.style.strokeDasharray = `${circumference}`;
  donutSegment.style.strokeDashoffset = `${circumference * (1 - ratio)}`;
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

function formatIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
