const TOKEN_KEY = "conecta21_token";

function salvarToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

function obterToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function removerToken() {
    localStorage.removeItem(TOKEN_KEY);
}

function estaAutenticado() {
    return obterToken() !== null;
}

function logout() {
    removerToken();
    window.location.href = "login.html";
}