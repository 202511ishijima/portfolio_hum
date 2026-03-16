window.Cart = (function () {
  const storageKey = "hamu_cart";

  function getCart() {
    return JSON.parse(localStorage.getItem(storageKey) || "[]");
  }

  function saveCart(cart) {
    localStorage.setItem(storageKey, JSON.stringify(cart));
    document.dispatchEvent(new Event("cart:updated"));
  }

  function addItem(item, quantity = 1) {
    const cart = getCart();
    const existing = cart.find((entry) => entry.id === item.id);
    if (existing) {
      existing.quantity += quantity;
    } else {
      cart.push({ ...item, quantity });
    }
    saveCart(cart);
  }

  function updateQuantity(id, quantity) {
    const cart = getCart()
      .map((item) => (item.id === id ? { ...item, quantity } : item))
      .filter((item) => item.quantity > 0);
    saveCart(cart);
  }

  function removeItem(id) {
    saveCart(getCart().filter((item) => item.id !== id));
  }

  function clear() {
    localStorage.removeItem(storageKey);
    document.dispatchEvent(new Event("cart:updated"));
  }

  function getSummary() {
    const cart = getCart();
    const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const shipping = subtotal > 0 ? 550 : 0;
    return { items: cart, subtotal, shipping, total: subtotal + shipping };
  }

  function renderCartPage() {
    const list = document.getElementById("cart-items");
    const summary = document.getElementById("cart-summary");
    if (!list || !summary) return;

    const data = getSummary();
    if (!data.items.length) {
      list.innerHTML = '<div class="empty-state">カートに商品が入っていません。商品一覧から追加してください。</div>';
      summary.innerHTML = '<div class="summary-card"><h2>ご購入について</h2><p>商品を追加すると、ここに合計金額が表示されます。</p><a class="button button--ghost" href="index.html">商品一覧へ</a></div>';
      return;
    }

    list.innerHTML = data.items.map((item) => `
      <article class="cart-item">
        <div>
          <h3>${item.name}</h3>
          <p class="text-soft">${item.categoryLabel}</p>
          <p class="price">${SiteRouter.formatPrice(item.price)}</p>
        </div>
        <div class="qty-row">
          <label>数量 <input type="number" min="1" value="${item.quantity}" data-cart-qty="${item.id}"></label>
          <button class="button button--ghost" type="button" data-cart-remove="${item.id}">削除</button>
        </div>
      </article>
    `).join("");

    summary.innerHTML = `
      <div class="summary-card">
        <h2>ご注文内容</h2>
        <p>小計 <strong>${SiteRouter.formatPrice(data.subtotal)}</strong></p>
        <p>送料 <strong>${SiteRouter.formatPrice(data.shipping)}</strong></p>
        <p>合計 <strong class="price">${SiteRouter.formatPrice(data.total)}</strong></p>
        <a class="button" href="checkout.html">購入手続きへ進む</a>
      </div>
    `;

    list.querySelectorAll("[data-cart-qty]").forEach((input) => {
      input.addEventListener("change", () => {
        updateQuantity(input.dataset.cartQty, Math.max(1, Number(input.value)));
        renderCartPage();
      });
    });

    list.querySelectorAll("[data-cart-remove]").forEach((button) => {
      button.addEventListener("click", () => {
        removeItem(button.dataset.cartRemove);
        renderCartPage();
      });
    });
  }

  function renderCheckoutPage() {
    const summary = document.getElementById("checkout-summary");
    const form = document.getElementById("checkout-form");
    if (!summary || !form) return;

    const data = getSummary();
    summary.innerHTML = `
      <div class="summary-card">
        <h2>ご注文内容</h2>
        <p>商品点数 <strong>${data.items.length}</strong></p>
        <p>小計 <strong>${SiteRouter.formatPrice(data.subtotal)}</strong></p>
        <p>送料 <strong>${SiteRouter.formatPrice(data.shipping)}</strong></p>
        <p>合計 <strong class="price">${SiteRouter.formatPrice(data.total)}</strong></p>
      </div>
    `;

    form.addEventListener("submit", (event) => {
      event.preventDefault();
      localStorage.setItem("hamu_last_order_total", String(data.total));
      clear();
      const completePath = document.body.dataset.completePath || "complete.html";
      window.location.href = completePath;
    });
  }

  function renderCompletePage() {
    const total = document.getElementById("complete-total");
    if (total) {
      const value = Number(localStorage.getItem("hamu_last_order_total") || 0);
      total.textContent = SiteRouter.formatPrice(value);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderCartPage();
    renderCheckoutPage();
    renderCompletePage();
  });

  return { addItem, getCart, getSummary, clear };
})();
