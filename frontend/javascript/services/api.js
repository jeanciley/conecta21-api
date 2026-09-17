const API_URL = "http://localhost:8080";

async function apiRequest(endpoint, options = {}) {

    const token = obterToken();

    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers
    });

    if (response.status === 401) {
        logout();
        return;
    }

    return response;
}