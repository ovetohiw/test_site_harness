(() => {
    const CART_KEY = "aether_cart_id";
    const productGrid = document.getElementById("product-grid");
    const errorEl = document.getElementById("error");
    const cartToggle = document.getElementById("cart-toggle");
    const cartClose = document.getElementById("cart-close");
    const cartPanel = document.getElementById("cart-panel");
    const cartList = document.getElementById("cart-list");
    const cartEmpty = document.getElementById("cart-empty");
    const cartFooter = document.getElementById("cart-footer");
    const cartTotal = document.getElementById("cart-total");
    const cartBadge = document.getElementById("cart-badge");

    function cartId() {
        let id = localStorage.getItem(CART_KEY);
        if (!id) {
            id = crypto.randomUUID();
            localStorage.setItem(CART_KEY, id);
        }
        return id;
    }

    function showError(message) {
        errorEl.hidden = !message;
        errorEl.textContent = message || "";
    }

    function formatPrice(value) {
        const amount = Number(value);
        return new Intl.NumberFormat("ru-RU", {
            style: "currency",
            currency: "RUB",
            maximumFractionDigits: 0
        }).format(amount);
    }

    async function api(path, options = {}) {
        const headers = {
            "X-Cart-Id": cartId(),
            ...(options.headers || {})
        };
        if (options.body) {
            headers["Content-Type"] = "application/json";
        }
        const response = await fetch(path, { ...options, headers });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || "Ошибка запроса");
        }
        return data;
    }

    function renderProducts(products) {
        productGrid.innerHTML = products.map((product) => `
            <article class="product" data-id="${product.id}">
                <img class="product-image" src="${product.imageUrl}" alt="${escapeHtml(product.name)}">
                <h2 class="product-name">${escapeHtml(product.name)}</h2>
                <p class="product-price">${formatPrice(product.price)}</p>
                <p class="product-desc">${escapeHtml(product.description)}</p>
                <button type="button" class="btn btn-primary add-btn" data-id="${product.id}">В корзину</button>
            </article>
        `).join("");

        productGrid.querySelectorAll(".add-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                try {
                    showError("");
                    const cart = await api("/api/cart/items", {
                        method: "POST",
                        body: JSON.stringify({ productId: Number(button.dataset.id), quantity: 1 })
                    });
                    renderCart(cart);
                    openCart(true);
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

    function openCart(open) {
        cartPanel.hidden = !open;
        cartToggle.setAttribute("aria-expanded", open ? "true" : "false");
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;");
    }

    cartToggle.addEventListener("click", () => openCart(cartPanel.hidden));
    cartClose.addEventListener("click", () => openCart(false));

    async function init() {
        try {
            const [products, cart] = await Promise.all([
                fetch("/api/products").then(async (response) => {
                    const data = await response.json();
                    if (!response.ok) {
                        throw new Error(data.message || "Не удалось загрузить товары");
                    }
                    return data;
                }),
                api("/api/cart")
            ]);
            renderProducts(products);
            renderCart(cart);
        } catch (err) {
            showError(err.message);
        }
    }

    init();
})();
