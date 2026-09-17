// =========================================
// EQUIPE - CONECTA21
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

const btnNovoUsuario =
    document.getElementById("btnNovoUsuario");

const btnCancelarUsuario =
    document.getElementById("btnCancelarUsuario");

const formUsuario =
    document.getElementById("formUsuario");

const usuarioForm =
    document.getElementById("usuarioForm");

const usuariosTableBody =
    document.getElementById("usuariosTableBody");

const totalUsuarios =
    document.getElementById("totalUsuarios");

const buscarUsuario =
    document.getElementById("buscarUsuario");

const filtroTipo =
    document.getElementById("filtroTipo");

const btnLogout =
    document.getElementById("btnLogout");


// Campos do formulário

const nomeUsuario =
    document.getElementById("nomeUsuario");

const emailUsuario =
    document.getElementById("emailUsuario");

const tipoUsuario =
    document.getElementById("tipoUsuario");

const senhaUsuario =
    document.getElementById("senhaUsuario");


// Mensagens de erro

const nomeUsuarioError =
    document.getElementById("nomeUsuarioError");

const emailUsuarioError =
    document.getElementById("emailUsuarioError");

const tipoUsuarioError =
    document.getElementById("tipoUsuarioError");

const senhaUsuarioError =
    document.getElementById("senhaUsuarioError");


// =========================================
// LISTA TEMPORÁRIA DE USUÁRIOS
// =========================================

let usuarios = [];


// =========================================
// ABRIR FORMULÁRIO
// =========================================

btnNovoUsuario.addEventListener("click", function () {

    formUsuario.hidden = false;

    nomeUsuario.focus();

});


// =========================================
// FECHAR FORMULÁRIO
// =========================================

btnCancelarUsuario.addEventListener("click", function () {

    usuarioForm.reset();

    limparErros();

    formUsuario.hidden = true;

});


// =========================================
// LIMPAR ERROS
// =========================================

function limparErros() {

    nomeUsuarioError.textContent = "";
    emailUsuarioError.textContent = "";
    tipoUsuarioError.textContent = "";
    senhaUsuarioError.textContent = "";

}


// =========================================
// VALIDAR FORMULÁRIO
// =========================================

function validarFormulario() {

    let valido = true;

    limparErros();


    // Nome

    if (nomeUsuario.value.trim() === "") {

        nomeUsuarioError.textContent =
            "O nome é obrigatório.";

        valido = false;

    }


    // E-mail

    const email = emailUsuario.value.trim();

    if (email === "") {

        emailUsuarioError.textContent =
            "O e-mail é obrigatório.";

        valido = false;

    } else if (
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    ) {

        emailUsuarioError.textContent =
            "Digite um e-mail válido.";

        valido = false;

    }


    // Tipo

    if (tipoUsuario.value === "") {

        tipoUsuarioError.textContent =
            "Selecione o tipo de usuário.";

        valido = false;

    }


    // Senha

    if (senhaUsuario.value.length < 8) {

        senhaUsuarioError.textContent =
            "A senha deve possuir pelo menos 8 caracteres.";

        valido = false;

    }


    return valido;

}


// =========================================
// CADASTRAR USUÁRIO
// =========================================

usuarioForm.addEventListener("submit", function (event) {

    event.preventDefault();


    if (!validarFormulario()) {

        return;

    }


    const novoUsuario = {

        nome: nomeUsuario.value.trim(),

        email: emailUsuario.value.trim(),

        tipo: tipoUsuario.value,

        status: "Ativo"

    };


    usuarios.push(novoUsuario);


    renderizarUsuarios();


    usuarioForm.reset();

    limparErros();

    formUsuario.hidden = true;

});


// =========================================
// EXIBIR USUÁRIOS
// =========================================

function renderizarUsuarios() {

    const termo =
        buscarUsuario.value.trim().toLowerCase();

    const tipoFiltro =
        filtroTipo.value;


    const usuariosFiltrados =
        usuarios.filter(function (usuario) {

            const correspondeBusca =
                usuario.nome.toLowerCase().includes(termo) ||
                usuario.email.toLowerCase().includes(termo);

            const correspondeTipo =
                tipoFiltro === "todos" ||
                usuario.tipo === tipoFiltro;

            return correspondeBusca && correspondeTipo;

        });


    usuariosTableBody.innerHTML = "";


    if (usuariosFiltrados.length === 0) {

        usuariosTableBody.innerHTML = `

            <tr>

                <td colspan="5">

                    <div class="empty-state">

                        <h3>
                            Nenhum usuário encontrado
                        </h3>

                        <p>
                            Cadastre um usuário para
                            começar a preencher a equipe.
                        </p>

                    </div>

                </td>

            </tr>

        `;

    } else {

        usuariosFiltrados.forEach(function (usuario) {

            const linha = document.createElement("tr");

            linha.innerHTML = `

    <td>
        <strong>${usuario.nome}</strong>
    </td>

    <td>
        ${usuario.email}
    </td>

    <td>

        <span class="user-type ${usuario.tipo}">

            ${usuario.tipo === "tecnico"
                ? "Técnico"
                : "Usuário"}

        </span>

    </td>

    <td>

        <span class="user-status">

            ${usuario.status}

        </span>

    </td>

    <td>

        <button
            type="button"
            class="btn btn-secondary btn-visualizar"
            data-email="${usuario.email}"
        >
            Visualizar
        </button>

    </td>

`;

            usuariosTableBody.appendChild(linha);

        });

    }


    atualizarContador();

}


// =========================================
// ATUALIZAR CONTADOR
// =========================================

function atualizarContador() {

    const quantidade = usuarios.length;

    totalUsuarios.textContent =
        quantidade === 1
            ? "1 usuário"
            : `${quantidade} usuários`;

}


// =========================================
// BUSCA E FILTRO
// =========================================

buscarUsuario.addEventListener(
    "input",
    renderizarUsuarios
);


filtroTipo.addEventListener(
    "change",
    renderizarUsuarios
);


// =========================================
// LOGOUT
// =========================================

btnLogout.addEventListener("click", function () {

    logout();

});


// =========================================
// INICIALIZAÇÃO
// =========================================

renderizarUsuarios();

// =========================================
// VISUALIZAR USUÁRIO
// =========================================

const modalUsuario =
    document.getElementById("modalUsuario");

const modalUsuarioTitulo =
    document.getElementById("modalUsuarioTitulo");

const modalNome =
    document.getElementById("modalNome");

const modalEmail =
    document.getElementById("modalEmail");

const modalTipo =
    document.getElementById("modalTipo");

const modalStatus =
    document.getElementById("modalStatus");

const btnFecharModal =
    document.getElementById("btnFecharModal");

const btnFecharModalFooter =
    document.getElementById("btnFecharModalFooter");


// =========================================
// ABRIR MODAL
// =========================================

usuariosTableBody.addEventListener("click", function (event) {

    if (!event.target.classList.contains("btn-visualizar")) {

        return;

    }


    const email =
        event.target.dataset.email;


    const usuario =
        usuarios.find(function (usuario) {

            return usuario.email === email;

        });


    if (!usuario) {

        return;

    }


    modalUsuarioTitulo.textContent =
        usuario.nome;

    modalNome.textContent =
        usuario.nome;

    modalEmail.textContent =
        usuario.email;

    modalTipo.textContent =
        usuario.tipo === "tecnico"
            ? "Técnico"
            : "Usuário";

    modalStatus.textContent =
        usuario.status;


    modalUsuario.hidden = false;

});


// =========================================
// FECHAR MODAL
// =========================================

function fecharModal() {

    modalUsuario.hidden = true;

}


btnFecharModal.addEventListener(
    "click",
    fecharModal
);


btnFecharModalFooter.addEventListener(
    "click",
    fecharModal
);