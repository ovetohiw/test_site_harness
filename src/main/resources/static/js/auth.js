(() => {
    const TOKEN_KEY = "perfume_shop_token";

    const form = document.getElementById("auth-form");
    const sessionView = document.getElementById("session-view");
    const sessionLogin = document.getElementById("session-login");
    const headline = document.getElementById("headline");
    const lede = document.getElementById("lede");
    const errorEl = document.getElementById("error");
    const loginInput = document.getElementById("login");
    const passwordInput = document.getElementById("password");
    const loginBtn = document.getElementById("login-btn");
    const registerBtn = document.getElementById("register-btn");
    const logoutBtn = document.getElementById("logout-btn");

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
    }

    function showError(message) {
        errorEl.hidden = false;
        errorEl.textContent = message;
    }

    function clearError() {
        errorEl.hidden = true;
        errorEl.textContent = "";
    }

    function showSession(login) {
        form.classList.add("hidden");
        sessionView.classList.remove("hidden");
        sessionLogin.textContent = login;
        headline.textContent = "С возвращением";
        lede.innerHTML = 'Профиль, избранное и история покупок — в <a href="/catalog.html">каталоге</a>.';
    }

    function showForm() {
        sessionView.classList.add("hidden");
        form.classList.remove("hidden");
        headline.textContent = "Войдите в кабинет";
        lede.innerHTML = 'Один аккаунт — доступ к каталогу и заказам. <a href="/catalog.html">Смотреть каталог</a> без входа.';
        passwordInput.value = "";
    }

    async function parseJsonSafe(response) {
        try {
            return await response.json();
        } catch (_) {
            return null;
        }
    }

    async function authRequest(path, body) {
        const response = await fetch(path, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        const data = await parseJsonSafe(response);
        if (!response.ok) {
            const message = data && data.message ? data.message : "Не удалось выполнить запрос";
            throw new Error(message);
        }
        return data;
    }

    async function restoreSession() {
        const token = getToken();
        if (!token) {
            showForm();
            return;
        }

        const response = await fetch("/api/auth/me", {
            headers: { Authorization: `Bearer ${token}` }
        });

        if (!response.ok) {
            clearToken();
            showForm();
            return;
        }

        const data = await parseJsonSafe(response);
        showSession(data.login);
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearError();
        loginBtn.disabled = true;
        try {
            const payload = {
                login: loginInput.value.trim(),
                password: passwordInput.value
            };
            const data = await authRequest("/api/auth/login", payload);
            setToken(data.token);
            showSession(data.login);
        } catch (err) {
            showError(err.message);
        } finally {
            loginBtn.disabled = false;
        }
    });

    registerBtn.addEventListener("click", async () => {
        clearError();
        registerBtn.disabled = true;
        try {
            const payload = {
                login: loginInput.value.trim(),
                password: passwordInput.value
            };
            const data = await authRequest("/api/auth/register", payload);
            setToken(data.token);
            showSession(data.login);
        } catch (err) {
            showError(err.message);
        } finally {
            registerBtn.disabled = false;
        }
    });

    logoutBtn.addEventListener("click", () => {
        clearToken();
        clearError();
        showForm();
    });

    restoreSession().catch(() => {
        clearToken();
        showForm();
    });
})();
