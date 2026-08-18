/*
    Mapa de Calor: gera os quadrados dos últimos ~12 meses,
    do domingo inicial até hoje.
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
dias.forEach(data => {
    const dia = document.createElement('div');

    dia.classList.add('day');

    // Data exibida ao passar o mouse
    dia.title = data.toLocaleDateString('pt-BR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });

    heatmap.appendChild(dia);
});