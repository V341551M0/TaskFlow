/*
    Mapa de Calor: gera os quadrados dos últimos ~12 meses
    (alinhados em semanas, de domingo a sábado) e os rótulos de mês.
*/
const heatmap = document.querySelector('#heatmap');
const months = document.querySelector('#months');

const hoje = new Date();

const inicio = new Date(hoje);
inicio.setDate(hoje.getDate() - 364);

// Volta para o domingo da semana inicial
inicio.setDate(inicio.getDate() - inicio.getDay());

const fim = new Date(hoje);

// Avança até o sábado da semana atual
fim.setDate(fim.getDate() + (6 - fim.getDay()));

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

// =============================
// CRIA OS MESES
// =============================
const semanas = Math.ceil(dias.length / 7);

months.style.gridTemplateColumns =
    `repeat(${semanas}, 12px)`;

let ultimoMes = -1;

dias.forEach((data, index) => {
    // Domingo = início da semana
    if (data.getDay() === 0) {
        const mes = data.getMonth();

        if (mes !== ultimoMes) {
            const nomeMes = document.createElement('span');

            nomeMes.textContent =
                data.toLocaleDateString('pt-BR', {
                    month: 'short'
                });

            nomeMes.style.gridColumn =
                `${Math.floor(index / 7) + 1}`;

            months.appendChild(nomeMes);

            ultimoMes = mes;
        }
    }
});