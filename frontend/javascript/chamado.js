// =========================================
// CHAMADOS - CONECTA21
// =========================================


// =========================================
// VERIFICAÇÃO DE AUTENTICAÇÃO
// =========================================

if (!estaAutenticado()) {

    window.location.href = "login.html";

}


// =========================================
// ELEMENTOS DO HTML
// =========================================

const btnNovoChamado =
    document.getElementById("btnNovoChamado");

const formNovoChamado =
    document.getElementById("formNovoChamado");

const btnCancelarChamado =
    document.getElementById("btnCancelarChamado");

const tituloChamado =
    document.getElementById("tituloChamado");

const clienteChamado =
    document.getElementById("clienteChamado");

const prioridadeChamado =
    document.getElementById("prioridadeChamado");

const responsavelChamado =
    document.getElementById("responsavelChamado");

const descricaoChamado =
    document.getElementById("descricaoChamado");

const tituloChamadoError =
    document.getElementById("tituloChamadoError");

const clienteChamadoError =
    document.getElementById("clienteChamadoError");

const prioridadeChamadoError =
    document.getElementById("prioridadeChamadoError");

const responsavelChamadoError =
    document.getElementById("responsavelChamadoError");

const descricaoChamadoError =
    document.getElementById("descricaoChamadoError");

const buscarChamado =
    document.getElementById("buscarChamado");

const filtroStatus =
    document.getElementById("filtroStatus");

const filtroPrioridade =
    document.getElementById("filtroPrioridade");

const chamadosTableBody =
    document.getElementById("chamadosTableBody");

const totalChamados =
    document.getElementById("totalChamados");

const btnLogout =
    document.getElementById("btnLogout");

const modalChamado =
    document.getElementById("modalChamado");

const modalChamadoTitulo =
    document.getElementById("modalChamadoTitulo");

const modalChamadoNumero =
    document.getElementById("modalChamadoNumero");

const modalChamadoCliente =
    document.getElementById("modalChamadoCliente");

const modalChamadoPrioridade =
    document.getElementById("modalChamadoPrioridade");

const modalChamadoStatus =
    document.getElementById("modalChamadoStatus");

const modalChamadoResponsavel =
    document.getElementById("modalChamadoResponsavel");

const modalChamadoDescricao =
    document.getElementById("modalChamadoDescricao");

const btnFecharModalChamado =
    document.getElementById("btnFecharModalChamado");

const btnFecharModalChamadoFooter =
    document.getElementById("btnFecharModalChamadoFooter");

const timelineChamado =
    document.getElementById("timelineChamado");


// =========================================
// LISTA TEMPORÁRIA DE CHAMADOS
// =========================================

let chamados = [    
    {
    numero: "CH-0001",
    titulo: "Computador não liga",
    cliente: "João da Silva",
    prioridade: "alta",
    status: "aberto",
    responsavel: "Carlos Oliveira"
},

{
    numero: "CH-0002",
    titulo: "Erro ao acessar o sistema",
    cliente: "Maria Souza",
    prioridade: "media",
    status: "andamento",
    responsavel: "Ana Costa"
},

{
    numero: "CH-0003",
    titulo: "Solicitação de instalação de software",
    cliente: "Empresa ABC",
    prioridade: "baixa",
    status: "aguardando",
    responsavel: "Carlos Oliveira"
}];

// =========================================
// DADOS TEMPORÁRIOS DA TIMELINE
// =========================================

const interacoesChamados = {

    "CH-0001": [

        {
            autor: "João da Silva",
            tipo: "Chamado aberto",
            data: "17/09/2026 09:15",
            descricao: "O cliente informou que o computador não liga."
        },

        {
            autor: "Carlos Oliveira",
            tipo: "Chamado atribuído",
            data: "17/09/2026 09:30",
            descricao: "O chamado foi atribuído ao técnico Carlos Oliveira."
        },

        {
            autor: "Carlos Oliveira",
            tipo: "Interação adicionada",
            data: "17/09/2026 10:00",
            descricao: "O técnico iniciou a análise do equipamento."
        }

    ]

};


// =========================================
// VALIDAÇÃO DO FORMULÁRIO
// =========================================

function validarFormulario() {

    let valido = true;


    // Limpa as mensagens anteriores

    tituloChamadoError.textContent = "";
    clienteChamadoError.textContent = "";
    prioridadeChamadoError.textContent = "";
    responsavelChamadoError.textContent = "";
    descricaoChamadoError.textContent = "";


    // Validação do título

    if (tituloChamado.value.trim() === "") {

        tituloChamadoError.textContent =
            "O título é obrigatório.";

        valido = false;

    }


    // Validação do cliente

    if (clienteChamado.value.trim() === "") {

        clienteChamadoError.textContent =
            "O cliente é obrigatório.";

        valido = false;

    }


    // Validação da prioridade

    if (prioridadeChamado.value === "") {

        prioridadeChamadoError.textContent =
            "Selecione uma prioridade.";

        valido = false;

    }


    // Validação do responsável

    if (responsavelChamado.value.trim() === "") {

        responsavelChamadoError.textContent =
            "O responsável é obrigatório.";

        valido = false;

    }


    // Validação da descrição

    if (descricaoChamado.value.trim() === "") {

        descricaoChamadoError.textContent =
            "A descrição é obrigatória.";

        valido = false;

    }


    return valido;

}


// =========================================
// ENVIO DO FORMULÁRIO
// =========================================

const novoChamadoForm =
    document.getElementById("novoChamadoForm");


novoChamadoForm.addEventListener("submit", function (event) {

    event.preventDefault();


    if (!validarFormulario()) {

        return;

    }


    // Gera o número do próximo chamado

    const proximoNumero =
        chamados.length + 1;

    const numeroChamado =
        `CH-${String(proximoNumero).padStart(4, "0")}`;


    // Cria o novo chamado

    const novoChamado = {

        numero: numeroChamado,

        titulo: tituloChamado.value.trim(),

        cliente: clienteChamado.value.trim(),

        prioridade: prioridadeChamado.value,

        status: "aberto",

        responsavel: responsavelChamado.value.trim(),

        descricao: descricaoChamado.value.trim()

    };


    // Adiciona o chamado à lista

    chamados.push(novoChamado);


    // Atualiza a tabela

    renderizarChamados();


    // Limpa o formulário

    novoChamadoForm.reset();


    // Limpa as mensagens de erro

    tituloChamadoError.textContent = "";

    clienteChamadoError.textContent = "";

    prioridadeChamadoError.textContent = "";

    responsavelChamadoError.textContent = "";

    descricaoChamadoError.textContent = "";


    // Fecha o formulário

    formNovoChamado.hidden = true;


    console.log(
        "Chamado cadastrado:",
        novoChamado
    );

});


// =========================================
// ABRIR FORMULÁRIO DE NOVO CHAMADO
// =========================================

btnNovoChamado.addEventListener("click", function () {

    formNovoChamado.hidden = false;

});

// =========================================
// CANCELAR NOVO CHAMADO
// =========================================

btnCancelarChamado.addEventListener("click", function () {

    novoChamadoForm.reset();

    tituloChamadoError.textContent = "";
    clienteChamadoError.textContent = "";
    prioridadeChamadoError.textContent = "";
    responsavelChamadoError.textContent = "";
    descricaoChamadoError.textContent = "";

    formNovoChamado.hidden = true;

});

// =========================================
// LOGOUT
// =========================================

btnLogout.addEventListener("click", function () {

    logout();

});

// =========================================
// EXIBIR CHAMADOS
// =========================================

function renderizarChamados() {

    chamadosTableBody.innerHTML = "";


    const termo =
        buscarChamado.value.trim().toLowerCase();

    const statusFiltro =
        filtroStatus.value;

    const prioridadeFiltro =
        filtroPrioridade.value;


    const chamadosFiltrados =
        chamados.filter(function (chamado) {

            const correspondeBusca =
                chamado.numero.toLowerCase().includes(termo) ||
                chamado.titulo.toLowerCase().includes(termo) ||
                chamado.cliente.toLowerCase().includes(termo);


            const correspondeStatus =
                statusFiltro === "todos" ||
                chamado.status === statusFiltro;


            const correspondePrioridade =
                prioridadeFiltro === "todas" ||
                chamado.prioridade === prioridadeFiltro;


            return (
                correspondeBusca &&
                correspondeStatus &&
                correspondePrioridade
            );

        });


    chamadosFiltrados.forEach(function (chamado) {

        const linha = document.createElement("tr");


        linha.innerHTML = `

            <td>
                <strong>${chamado.numero}</strong>
            </td>

            <td>
                ${chamado.titulo}
            </td>

            <td>
                ${chamado.cliente}
            </td>

            <td>

    <span class="ticket-priority ${chamado.prioridade}">

        ${chamado.prioridade === "baixa"
            ? "Baixa"
            : chamado.prioridade === "media"
                ? "Média"
                : chamado.prioridade === "alta"
                    ? "Alta"
                    : "Crítica"}

    </span>

</td>

<td>

    <span class="ticket-status ${chamado.status}">

        ${chamado.status === "aberto"
            ? "Aberto"
            : chamado.status === "andamento"
                ? "Em andamento"
                : chamado.status === "aguardando"
                    ? "Aguardando"
                    : chamado.status === "resolvido"
                        ? "Resolvido"
                        : "Fechado"}

    </span>

</td>

            <td>
                ${chamado.responsavel}
            </td>

            <td>

<button
    type="button"
    class="btn btn-secondary btn-visualizar-chamado"
    data-numero="${chamado.numero}"
>
    Visualizar
</button>

            </td>

        `;


        chamadosTableBody.appendChild(linha);

    });


    atualizarContador(chamadosFiltrados.length);

}


// =========================================
// ATUALIZAR CONTADOR
// =========================================

function atualizarContador(quantidade) {

    totalChamados.textContent =
        quantidade === 1
            ? "1 chamado"
            : `${quantidade} chamados`;

}

// =========================================
// BUSCA E FILTROS
// =========================================

buscarChamado.addEventListener(
    "input",
    renderizarChamados
);


filtroStatus.addEventListener(
    "change",
    renderizarChamados
);


filtroPrioridade.addEventListener(
    "change",
    renderizarChamados
);

// =========================================
// INICIALIZAÇÃO
// =========================================

renderizarChamados();

console.log("Módulo de chamados carregado.");

// =========================================
// MODAL DE DETALHES DO CHAMADO
// =========================================

chamadosTableBody.addEventListener("click", function (event) {

    if (!event.target.classList.contains("btn-visualizar-chamado")) {
        return;
    }

    const numero = event.target.dataset.numero;

    const chamado = chamados.find(function (chamado) {
        return chamado.numero === numero;
    });

    if (!chamado) {
        return;
    }
    
    
    // Preenche os dados do chamado no modal
    
    modalChamadoTitulo.textContent =
        chamado.titulo;
    
    modalChamadoNumero.textContent =
        chamado.numero;
    
    modalChamadoCliente.textContent =
        chamado.cliente;
    
    modalChamadoPrioridade.textContent =
        chamado.prioridade === "baixa"
            ? "Baixa"
            : chamado.prioridade === "media"
                ? "Média"
                : chamado.prioridade === "alta"
                    ? "Alta"
                    : "Crítica";
    
    modalChamadoPrioridade.className =
        `ticket-priority ${chamado.prioridade}`;
    
    modalChamadoStatus.textContent =
        chamado.status === "aberto"
            ? "Aberto"
            : chamado.status === "andamento"
                ? "Em andamento"
                : chamado.status === "aguardando"
                    ? "Aguardando"
                    : chamado.status === "resolvido"
                        ? "Resolvido"
                        : "Fechado";
    
    modalChamadoStatus.className =
        `ticket-status ${chamado.status}`;
    
    modalChamadoResponsavel.textContent =
        chamado.responsavel;
    
    modalChamadoDescricao.textContent =
        chamado.descricao || "Descrição não informada";

    renderizarTimeline(chamado.numero);
    
    
    // Abre o modal
    
    modalChamado.hidden = false;

});

// =========================================
// FECHAR MODAL DE DETALHES
// =========================================

function fecharModalChamado() {

    modalChamado.hidden = true;

}


btnFecharModalChamado.addEventListener(
    "click",
    fecharModalChamado
);


btnFecharModalChamadoFooter.addEventListener(
    "click",
    fecharModalChamado
);

// =========================================
// RENDERIZAR TIMELINE DE INTERAÇÕES
// =========================================

function renderizarTimeline(numeroChamado) {

    const interacoes =
        interacoesChamados[numeroChamado] || [];


    timelineChamado.innerHTML = "";


    if (interacoes.length === 0) {

        timelineChamado.innerHTML = `
            <div class="timeline-empty">

                <p>
                    Nenhuma interação registrada.
                </p>

            </div>
        `;

        return;

    }


    interacoes.forEach(function (interacao) {

        const item =
            document.createElement("div");

        item.className = "timeline-item";


        item.innerHTML = `

            <div class="timeline-marker"></div>

            <div class="timeline-content">

                <div class="timeline-item-header">

                    <strong>
                        ${interacao.tipo}
                    </strong>

                    <span>
                        ${interacao.data}
                    </span>

                </div>


                <p class="timeline-author">
                    ${interacao.autor}
                </p>


                <p class="timeline-description">
                    ${interacao.descricao}
                </p>

            </div>

        `;


        timelineChamado.appendChild(item);

    });

}