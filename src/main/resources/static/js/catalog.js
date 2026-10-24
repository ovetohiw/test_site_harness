(() => {
    const CART_KEY = "aether_cart_id";
    const TOKEN_KEY = "perfume_shop_token";

    const productGrid = document.getElementById("product-grid");
    const errorEl = document.getElementById("error");
    const sortField = document.getElementById("sort-field");
    const sortDir = document.getElementById("sort-dir");
    const cartToggle = document.getElementById("cart-toggle");
    const cartClose = document.getElementById("cart-close");
    const cartPanel = document.getElementById("cart-panel");
    const cartList = document.getElementById("cart-list");
    const cartEmpty = document.getElementById("cart-empty");
    const cartFooter = document.getElementById("cart-footer");
    const cartTotal = document.getElementById("cart-total");
    const cartBadge = document.getElementById("cart-badge");
    const checkoutBtn = document.getElementById("checkout-btn");
    const checkoutHint = document.getElementById("checkout-hint");
    const authToggle = document.getElementById("auth-toggle");
    const authClose = document.getElementById("auth-close");
    const authPanel = document.getElementById("auth-panel");
    const authForm = document.getElementById("auth-form");
    const authError = document.getElementById("auth-error");
    const loginInput = document.getElementById("login");
    const passwordInput = document.getElementById("password");
    const loginBtn = document.getElementById("login-btn");
    const registerBtn = document.getElementById("register-btn");
    const profileToggle = document.getElementById("profile-toggle");
    const profileClose = document.getElementById("profile-close");
    const profilePanel = document.getElementById("profile-panel");
    const navAvatar = document.getElementById("nav-avatar");
    const navLogin = document.getElementById("nav-login");
    const profileAvatar = document.getElementById("profile-avatar");
    const profileLogin = document.getElementById("profile-login");
    const profileMeta = document.getElementById("profile-meta");
    const favoritesList = document.getElementById("favorites-list");
    const favoritesEmpty = document.getElementById("favorites-empty");
    const ordersList = document.getElementById("orders-list");
    const ordersEmpty = document.getElementById("orders-empty");
    const logoutBtn = document.getElementById("logout-btn");

    let currentUser = null;
    let favoriteIds = new Set();

    function cartId() {
        let id = localStorage.getItem(CART_KEY);
        if (!id) {
            id = crypto.randomUUID();
            localStorage.setItem(CART_KEY, id);
        }
        return id;
    }

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
        errorEl.hidden = !message;
        errorEl.textContent = message || "";
    }

    function showAuthError(message) {
        authError.hidden = !message;
        authError.textContent = message || "";
    }

    function formatPrice(value) {
        const amount = Number(value);
        return new Intl.NumberFormat("ru-RU", {
            style: "currency",
            currency: "RUB",
            maximumFractionDigits: 0
        }).format(amount);
    }

    function formatDate(value) {
        return new Intl.DateTimeFormat("ru-RU", {
            day: "numeric",
            month: "long",
            year: "numeric"
        }).format(new Date(value));
    }

    async function api(path, options = {}) {
        const headers = {
            "X-Cart-Id": cartId(),
            ...(options.headers || {})
        };
        if (options.body) {
            headers["Content-Type"] = "application/json";
        }
        const token = getToken();
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
        const response = await fetch(path, { ...options, headers });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || "Ошибка запроса");
        }
        return data;
    }

    function renderProducts(products) {
        productGrid.innerHTML = products.map((product) => {
            const favored = favoriteIds.has(product.id);
            return `
            <article class="product" data-id="${product.id}">
                <img class="product-image" src="${product.imageUrl}" alt="${escapeHtml(product.name)}">
                <p class="product-brand">${escapeHtml(product.brand)}</p>
                <h2 class="product-name">${escapeHtml(product.name)}</h2>
                <p class="product-price">${formatPrice(product.price)}</p>
                <p class="product-desc">${escapeHtml(product.description)}</p>
                <div class="product-actions">
                    <label class="qty-field">
                        <span>Кол-во</span>
                        <input class="qty-input" type="number" min="1" max="99" value="1" data-id="${product.id}" aria-label="Количество ${escapeHtml(product.name)}">
                    </label>
                    <button type="button" class="btn btn-primary add-btn" data-id="${product.id}">В корзину</button>
                    ${currentUser ? `<button type="button" class="fav-btn ${favored ? "is-on" : ""}" data-id="${product.id}" aria-pressed="${favored}" aria-label="Избранное">${favored ? "♥" : "♡"}</button>` : ""}
                </div>
            </article>`;
        }).join("");

        productGrid.querySelectorAll(".add-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                try {
                    showError("");
                    const qtyInput = productGrid.querySelector(`.qty-input[data-id="${button.dataset.id}"]`);
                    const quantity = Math.max(1, Math.min(99, Number(qtyInput?.value) || 1));
                    const cart = await api("/api/cart/items", {
                        method: "POST",
                        body: JSON.stringify({ productId: Number(button.dataset.id), quantity })
                    });
                    renderCart(cart);
                    openCart(true);
                } catch (err) {
                    showError(err.message);
                }
            });
        });

        productGrid.querySelectorAll(".fav-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                try {
                    showError("");
                    const productId = Number(button.dataset.id);
                    const on = favoriteIds.has(productId);
                    const favorites = on
                        ? await api(`/api/favorites/${productId}`, { method: "DELETE" })
                        : await api("/api/favorites", {
                            method: "POST",
                            body: JSON.stringify({ productId })
                        });
                    favoriteIds = new Set(favorites.map((item) => item.id));
                    renderFavorites(favorites);
                    renderProducts(products);
                } catch (err) {
                    showError(err.message);
                }
            });
        });
    }

    function renderCart(cart) {
        const items = cart.items || [];
        const count = items.reduce((sum, item) => sum + item.quantity, 0);
        cartBadge.textContent = String(count);

        if (items.length === 0) {
            cartList.innerHTML = "";
            cartEmpty.hidden = false;
            cartFooter.hidden = true;
            return;
        }

        cartEmpty.hidden = true;
        cartFooter.hidden = false;
        cartTotal.textContent = formatPrice(cart.total);
        checkoutBtn.hidden = !currentUser;
        checkoutHint.hidden = !!currentUser;

        cartList.innerHTML = items.map((item) => `
            <li class="cart-item">
                <img src="${item.imageUrl}" alt="${escapeHtml(item.name)}">
                <div>
                    <p class="cart-item-name">${escapeHtml(item.name)}</p>
                    <p class="cart-item-meta">${item.quantity} × ${formatPrice(item.price)}</p>
                </div>
                <button type="button" class="btn btn-ghost remove-btn" data-id="${item.productId}">Удалить</button>
            </li>
        `).join("");

        cartList.querySelectorAll(".remove-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                try {
                    showError("");
                    const next = await api(`/api/cart/items/${button.dataset.id}`, { method: "DELETE" });
                    renderCart(next);
                } catch (err) {
                    showError(err.message);
                }
            });
        });
    }

    function renderFavorites(favorites) {
        if (!favorites || favorites.length === 0) {
            favoritesList.innerHTML = "";
            favoritesEmpty.hidden = false;
            return;
        }
        favoritesEmpty.hidden = true;
        favoritesList.innerHTML = favorites.map((item) => `
            <li class="profile-item">
                <img src="${item.imageUrl}" alt="">
                <div>
                    <p class="profile-item-title">${escapeHtml(item.name)}</p>
                    <p class="profile-item-meta">${escapeHtml(item.brand)} · ${formatPrice(item.price)}</p>
                </div>
                <button type="button" class="btn btn-ghost unfav-btn" data-id="${item.id}">Убрать</button>
            </li>
        `).join("");

        favoritesList.querySelectorAll(".unfav-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                try {
                    showError("");
                    const favoritesNext = await api(`/api/favorites/${button.dataset.id}`, { method: "DELETE" });
                    favoriteIds = new Set(favoritesNext.map((item) => item.id));
                    renderFavorites(favoritesNext);
                    await loadProducts();
                } catch (err) {
                    showError(err.message);
                }
            });
        });
    }

    function renderOrders(orders) {
        if (!orders || orders.length === 0) {
            ordersList.innerHTML = "";
            ordersEmpty.hidden = false;
            return;
        }
        ordersEmpty.hidden = true;
        ordersList.innerHTML = orders.map((order) => {
            const lines = (order.items || [])
                .map((item) => `${escapeHtml(item.productName)} × ${item.quantity}`)
                .join(", ");
            return `
            <li class="profile-item profile-order">
                <div>
                    <p class="profile-item-title">Заказ от ${formatDate(order.createdAt)}</p>
                    <p class="profile-item-meta">${lines}</p>
                </div>
                <strong>${formatPrice(order.total)}</strong>
            </li>`;
        }).join("");
    }

    function setAuthUi(user) {
        currentUser = user;
        if (user) {
            authToggle.hidden = true;
            profileToggle.hidden = false;
            navAvatar.src = user.avatarUrl || "/images/avatar-default.svg";
            navLogin.textContent = user.login;
            profileAvatar.src = user.avatarUrl || "/images/avatar-default.svg";
            profileLogin.textContent = user.login;
            profileMeta.textContent = user.createdAt
                ? `С нами с ${formatDate(user.createdAt)}`
                : "";
            openAuth(false);
        } else {
            authToggle.hidden = false;
            profileToggle.hidden = true;
            openProfile(false);
            favoriteIds = new Set();
        }
    }

    function openCart(open) {
        cartPanel.hidden = !open;
        cartToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) {
            openProfile(false);
            openAuth(false);
        }
    }

    function openProfile(open) {
        profilePanel.hidden = !open;
        profileToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) {
            openCart(false);
            openAuth(false);
        }
    }

    function openAuth(open) {
        authPanel.hidden = !open;
        authToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) {
            openCart(false);
            openProfile(false);
            showAuthError("");
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;");
    }

    async function loadProducts() {
        const sort = sortField.value;
        const dir = sortDir.value;
        const products = await fetch(`/api/products?sort=${encodeURIComponent(sort)}&dir=${encodeURIComponent(dir)}`)
            .then(async (response) => {
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.message || "Не удалось загрузить товары");
                }
                return data;
            });
        renderProducts(products);
    }

    async function loadProfileData() {
        if (!currentUser) {
            return;
        }
        const [favorites, orders] = await Promise.all([
            api("/api/favorites"),
            api("/api/orders")
        ]);
        favoriteIds = new Set(favorites.map((item) => item.id));
        renderFavorites(favorites);
        renderOrders(orders);
    }

    async function restoreSession() {
        const token = getToken();
        if (!token) {
            setAuthUi(null);
            return;
        }
        const response = await fetch("/api/auth/me", {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (!response.ok) {
            clearToken();
            setAuthUi(null);
            return;
        }
        const user = await response.json();
        setAuthUi(user);
        await loadProfileData();
    }

    async function completeAuth(data) {
        setToken(data.token);
        await restoreSession();
        const cart = await api("/api/cart");
        renderCart(cart);
        await loadProducts();
        openProfile(true);
    }

    cartToggle.addEventListener("click", () => openCart(cartPanel.hidden));
    cartClose.addEventListener("click", () => openCart(false));
    profileToggle.addEventListener("click", () => openProfile(profilePanel.hidden));
    profileClose.addEventListener("click", () => openProfile(false));
    authToggle.addEventListener("click", () => openAuth(authPanel.hidden));
    authClose.addEventListener("click", () => openAuth(false));
    sortField.addEventListener("change", () => loadProducts().catch((err) => showError(err.message)));
    sortDir.addEventListener("change", () => loadProducts().catch((err) => showError(err.message)));

    checkoutHint.addEventListener("click", () => {
        if (!currentUser) {
            openAuth(true);
        }
    });

    authForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        showAuthError("");
        loginBtn.disabled = true;
        try {
            const payload = {
                login: loginInput.value.trim(),
                password: passwordInput.value
            };
            const data = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            await completeAuth(data);
            passwordInput.value = "";
        } catch (err) {
            showAuthError(err.message);
        } finally {
            loginBtn.disabled = false;
        }
    });

    registerBtn.addEventListener("click", async () => {
        showAuthError("");
        registerBtn.disabled = true;
        try {
            const payload = {
                login: loginInput.value.trim(),
                password: passwordInput.value
            };
            const data = await api("/api/auth/register", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            await completeAuth(data);
            passwordInput.value = "";
        } catch (err) {
            showAuthError(err.message);
        } finally {
            registerBtn.disabled = false;
        }
    });

    checkoutBtn.addEventListener("click", async () => {
        try {
            showError("");
            await api("/api/orders/checkout", { method: "POST" });
            const cart = await api("/api/cart");
            renderCart(cart);
            await loadProfileData();
            openCart(false);
            openProfile(true);
        } catch (err) {
            showError(err.message);
        }
    });

    logoutBtn.addEventListener("click", () => {
        clearToken();
        setAuthUi(null);
        loadProducts().catch((err) => showError(err.message));
        openAuth(false);
    });

    async function init() {
        try {
            await restoreSession();
            const cart = await api("/api/cart");
            await loadProducts();
            renderCart(cart);
        } catch (err) {
            showError(err.message);
        }
    }

    init();
})();
