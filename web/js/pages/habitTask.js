/*
    Mapa de Calor: gera os quadrados dos últimos ~12 meses,
    do domingo inicial até hoje, e pinta conclusões e falhas.
*/
const heatmap = document.querySelector('#heatmap');

const hoje = new Date();

const inicio = new Date(hoje);
inicio.setDate(hoje.getDate() - 364);

// Volta para o domingo da semana inicial
inicio.setDate(inicio.getDate() - inicio.getDay());

const fim = new Date(hoje);

const dias = [];

let dataAtual = new Date(inicio);

while (dataAtual <= fim) {
    dias.push(new Date(dataAtual));
    dataAtual.setDate(dataAtual.getDate() + 1);
}

// =============================
// CRIA OS DIAS
// =============================
const celulas = dias.map(data => {
    const dia = document.createElement('div');

    dia.classList.add('day');

    // Data exibida ao passar o mouse
    dia.title = data.toLocaleDateString('pt-BR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });

    heatmap.appendChild(dia);

    return { dia, data };
});

// =============================
// PINTA CONCLUSÕES E FALHAS
// =============================
async function colorirHeatmap(dadosCarregados) {
    const dados = dadosCarregados || await carregarHeatmap();

    celulas.forEach(({ dia, data }) => {
        dia.classList.remove('level-0', 'level-1', 'level-2', 'level-3', 'level-4', 'level-failed');

        const valor = Number(dados[formatarData(data)] || 0);

        if (valor > 0) {
            const nivel = Math.min(4, Math.max(1, Math.ceil(valor)));
            dia.classList.add(`level-${nivel}`);
        } else if (valor < 0) {
            dia.classList.add('level-failed');
        }
    });
}

async function carregarHeatmap() {
    try {
        const token = localStorage.getItem('taskflow-token') || '';
        const resposta = await fetch((window.TASKFLOW_API_URL || '') + '/api/heatmap', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (resposta.status === 401) {
            localStorage.removeItem('taskflow-auth');
            localStorage.removeItem('taskflow-token');
            window.location.replace('login.html');
            return {};
        }
        if (resposta.ok) {
            return await resposta.json();
        }
    } catch (erro) {
        console.warn('API indisponível para o heatmap, usando dados locais.', erro);
    }

    try {
        const estado = JSON.parse(localStorage.getItem('taskflow-state') || '{}');
        return estado.heatmap || {};
    } catch (erro) {
        return {};
    }
}

function formatarData(data) {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const diaMes = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${diaMes}`;
}

// Expõe a função para que o app.js a chame após cada atualização
window.refreshHeatmap = colorirHeatmap;

colorirHeatmap();