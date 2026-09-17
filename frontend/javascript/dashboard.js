// =========================================
// DASHBOARD - CONECTA21
// =========================================


// =========================================
// VERIFICAÇÃO DE AUTENTICAÇÃO
// =========================================

if (!estaAutenticado()) {

    window.location.href = "login.html";

}


// =========================================
// BOTÃO SAIR
// =========================================

const btnLogout = document.getElementById("btnLogout");


btnLogout.addEventListener("click", function () {

    logout();

});