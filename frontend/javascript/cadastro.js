// ========================================
// ELEMENTOS DO FORMULÁRIO
// ========================================

const cadastroForm = document.getElementById("cadastroForm");

const nome = document.getElementById("nome");
const email = document.getElementById("email");
const senha = document.getElementById("senha");
const confirmarSenha = document.getElementById("confirmarSenha");
const termos = document.getElementById("termos");


// ========================================
// ELEMENTOS DE ERRO
// ========================================

const nomeError = document.getElementById("nomeError");
const emailError = document.getElementById("emailError");
const senhaError = document.getElementById("senhaError");
const confirmarSenhaError = document.getElementById("confirmarSenhaError");
const termosError = document.getElementById("termosError");


// ========================================
// FUNÇÕES AUXILIARES
// ========================================

function mostrarErro(input, elementoErro, mensagem) {

    input.classList.add("input-error");
    input.classList.remove("input-success");

    elementoErro.textContent = mensagem;
}


function mostrarSucesso(input, elementoErro) {

    input.classList.remove("input-error");
    input.classList.add("input-success");

    elementoErro.textContent = "";
}


function limparEstado(input, elementoErro) {

    input.classList.remove("input-error");
    input.classList.remove("input-success");

    elementoErro.textContent = "";
}


// ========================================
// VALIDAR NOME
// ========================================

function validarNome() {

    const valor = nome.value.trim();

    if (valor === "") {

        mostrarErro(
            nome,
            nomeError,
            "Informe seu nome completo."
        );

        return false;
    }

    if (valor.length < 3) {

        mostrarErro(
            nome,
            nomeError,
            "O nome deve possuir pelo menos 3 caracteres."
        );

        return false;
    }

    mostrarSucesso(nome, nomeError);

    return true;
}


// ========================================
// VALIDAR E-MAIL
// ========================================

function validarEmail() {

    const valor = email.value.trim();

    if (valor === "") {

        mostrarErro(
            email,
            emailError,
            "Informe seu e-mail."
        );

        return false;
    }

    const emailValido =
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailValido.test(valor)) {

        mostrarErro(
            email,
            emailError,
            "Informe um e-mail válido."
        );

        return false;
    }

    mostrarSucesso(email, emailError);

    return true;
}


// ========================================
// VALIDAR SENHA
// ========================================

function validarSenha() {

    const valor = senha.value;

    if (valor === "") {

        mostrarErro(
            senha,
            senhaError,
            "Informe uma senha."
        );

        return false;
    }

    if (valor.length < 8) {

        mostrarErro(
            senha,
            senhaError,
            "A senha deve possuir pelo menos 8 caracteres."
        );

        return false;
    }

    mostrarSucesso(senha, senhaError);

    return true;
}


// ========================================
// VALIDAR CONFIRMAÇÃO DA SENHA
// ========================================

function validarConfirmacaoSenha() {

    const valor = confirmarSenha.value;

    if (valor === "") {

        mostrarErro(
            confirmarSenha,
            confirmarSenhaError,
            "Confirme sua senha."
        );

        return false;
    }

    if (valor !== senha.value) {

        mostrarErro(
            confirmarSenha,
            confirmarSenhaError,
            "As senhas não são iguais."
        );

        return false;
    }

    mostrarSucesso(
        confirmarSenha,
        confirmarSenhaError
    );

    return true;
}


// ========================================
// VALIDAR TERMOS
// ========================================

function validarTermos() {

    if (!termos.checked) {

        termosError.textContent =
            "Você precisa aceitar os termos para continuar.";

        return false;
    }

    termosError.textContent = "";

    return true;
}


// ========================================
// EVENTOS EM TEMPO REAL
// ========================================

nome.addEventListener("blur", validarNome);

email.addEventListener("blur", validarEmail);

senha.addEventListener("blur", validarSenha);

confirmarSenha.addEventListener(
    "blur",
    validarConfirmacaoSenha
);

senha.addEventListener(
    "input",
    () => {

        if (confirmarSenha.value !== "") {
            validarConfirmacaoSenha();
        }

    }
);


// ========================================
// ENVIO DO FORMULÁRIO
// ========================================

cadastroForm.addEventListener(
    "submit",
    function (event) {

        event.preventDefault();

        const nomeValido = validarNome();
        const emailValido = validarEmail();
        const senhaValida = validarSenha();
        const confirmacaoValida =
            validarConfirmacaoSenha();
        const termosValidos = validarTermos();


        if (
            !nomeValido ||
            !emailValido ||
            !senhaValida ||
            !confirmacaoValida ||
            !termosValidos
        ) {

            return;
        }


        // ========================================
        // TEMPORÁRIO
        // ========================================
        // Nesta etapa ainda não enviaremos
        // os dados para o Backend.
        //
        // Isso será implementado quando
        // criarmos o services/api.js.


        alert(
            "Cadastro validado com sucesso!"
        );

        cadastroForm.reset();

        limparEstado(nome, nomeError);
        limparEstado(email, emailError);
        limparEstado(senha, senhaError);
        limparEstado(
            confirmarSenha,
            confirmarSenhaError
        );

    }
);