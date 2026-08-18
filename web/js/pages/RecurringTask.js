const heatmap = document.querySelector("#heatmap");

const hoje = new Date();

for (let i = 364; i >= 0; i--) {
    const data = new Date(hoje);
    data.setDate(hoje.getDate() - 1);

    const dia = document.createElement("div");

    dia.classList.add("day");

    dia.title = data.toLocaleDateString("pt-BR");

    heatmap.appendChild(dia);
}