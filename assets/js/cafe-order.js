window.CafeOrderPage = (function () {
  const backendBaseUrl = "http://localhost:8080";
  const warningThresholdSeconds = 5 * 60;
  let menuCache = [];
  let selectedCounts = {};
  let sessionToken = "";
  let sessionInfo = null;
  let statusTimer = null;
  let sessionClockTimer = null;

  function yen(value) {
    return `${Number(value || 0).toLocaleString("ja-JP")}円`;
  }

  function setStatus(message, isError) {
    const el = document.getElementById("cafe-order-status");
    if (!el) return;
    const text = String(message || "").trim();
    el.textContent = text;
    el.className = "form-status";
    if (!text) {
      el.hidden = true;
      el.removeAttribute("data-state");
      return;
    }
    el.hidden = false;
    el.dataset.state = isError ? "error" : "success";
  }

  function getSessionTokenFromQuery() {
    const session = SiteRouter.getQueryParam("session");
    return session ? String(session).trim() : "";
  }

  function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("ja-JP", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });
  }

  function formatRemaining(seconds) {
    const safe = Math.max(0, Number(seconds || 0));
    const m = Math.floor(safe / 60);
    const s = safe % 60;
    return `${m}分${String(s).padStart(2, "0")}秒`;
  }

  function setOrderEnabled(enabled) {
    const submit = document.getElementById("bottom-order-button");
    if (submit) {
      submit.disabled = !enabled;
    }
    document.querySelectorAll("[data-order-card]").forEach((card) => {
      card.classList.toggle("is-disabled", !enabled);
    });
  }

  function updateSessionSummary() {
    const el = document.getElementById("cafe-session-summary");
    if (!el) return;
    if (!sessionInfo) {
      el.textContent = "セッション情報を取得できませんでした。";
      return;
    }

    const remainingSeconds = getRemainingSecondsLocal();
    const remainingText = formatRemaining(remainingSeconds);
    const base = `座席: ${sessionInfo.seatNo} / 有効期限: ${formatDateTime(sessionInfo.expiresAt)} / 残り: ${remainingText}まで注文できます`;

    const canOrderNow = sessionInfo.status === "ACTIVE" && remainingSeconds > 0;

    if (!canOrderNow) {
      el.textContent = base;
      setStatus("", false);
      setOrderEnabled(false);
      return;
    }

    if (remainingSeconds <= warningThresholdSeconds) {
      el.textContent = `${base} / ラストオーダー5分前です。`;
      setStatus("ラストオーダー5分前です。", true);
    } else {
      el.textContent = base;
      setStatus("", false);
    }

    setOrderEnabled(true);
  }

  async function fetchOrderMenu() {
    const response = await fetch(`${backendBaseUrl}/api/cafe/order-menu?session=${encodeURIComponent(sessionToken)}`);
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(payload.message || "セッション確認に失敗しました。");
    }
    return payload;
  }

  async function fetchSessionStatus() {
    const response = await fetch(`${backendBaseUrl}/api/cafe/sessions/${encodeURIComponent(sessionToken)}`);
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(payload.message || "セッション状態の更新に失敗しました。");
    }
    return payload;
  }

  async function fetchOrderHistory() {
    const response = await fetch(`${backendBaseUrl}/api/cafe/orders/history?session=${encodeURIComponent(sessionToken)}`);
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(payload.message || "注文履歴の取得に失敗しました。");
    }
    return payload;
  }

  function renderMenuCard(item) {
    const media = item.image
      ? `<figure class="media-photo"><img src="${SiteRouter.getBasePath()}image/${item.image}" alt="${item.name}"></figure>`
      : `<div class="media-placeholder"></div>`;

    return `
      <article class="menu-card cafe-order-card" data-order-card="${item.id}" role="button" tabindex="0" aria-label="${item.name}を1つ追加">
        ${media}
        <div class="cafe-order-card__body">
          <div class="cafe-order-card__row">
            <h3>${item.name}</h3>
            <div class="cafe-order-card__counter">
              <button class="cafe-order-card__step" type="button" data-order-minus="${item.id}" aria-label="${item.name}を1つ減らす">-</button>
              <span class="cafe-order-card__count" data-order-count="${item.id}">0</span>
              <button class="cafe-order-card__step" type="button" data-order-plus="${item.id}" aria-label="${item.name}を1つ増やす">+</button>
            </div>
          </div>
          <p class="price">${yen(item.price)}</p>
        </div>
      </article>
    `;
  }

  function renderMenus(menu) {
    const drinkTarget = document.getElementById("cafe-order-menu-drink");
    const foodTarget = document.getElementById("cafe-order-menu-food");
    if (!drinkTarget || !foodTarget) return;

    const drinks = menu.filter((item) => String(item.category || item.type || "").toUpperCase() === "DRINK");
    const foods = menu.filter((item) => {
      const category = String(item.category || item.type || "").toUpperCase();
      return category === "FOOD" || category === "TREAT";
    });

    drinkTarget.innerHTML = drinks.length ? drinks.map(renderMenuCard).join("") : "<p>ドリンクメニューはありません。</p>";
    foodTarget.innerHTML = foods.length ? foods.map(renderMenuCard).join("") : "<p>フードメニューはありません。</p>";
    bindMenuCardSelection();
    bindCounterButtons();
    refreshOrderCounts();
  }

  function bindMenuCardSelection() {
    document.querySelectorAll("[data-order-card]").forEach((card) => {
      card.addEventListener("click", () => incrementMenuCount(card.dataset.orderCard));
      card.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          incrementMenuCount(card.dataset.orderCard);
        }
      });
    });
  }

  function bindCounterButtons() {
    document.querySelectorAll("[data-order-plus]").forEach((button) => {
      button.addEventListener("click", (event) => {
        event.stopPropagation();
        incrementMenuCount(button.dataset.orderPlus);
      });
    });

    document.querySelectorAll("[data-order-minus]").forEach((button) => {
      button.addEventListener("click", (event) => {
        event.stopPropagation();
        decrementMenuCount(button.dataset.orderMinus);
      });
    });
  }

  function incrementMenuCount(menuId) {
    selectedCounts[menuId] = Number(selectedCounts[menuId] || 0) + 1;
    refreshOrderCounts();
  }

  function decrementMenuCount(menuId) {
    const current = Number(selectedCounts[menuId] || 0);
    if (current <= 0) return;
    selectedCounts[menuId] = current - 1;
    if (selectedCounts[menuId] <= 0) {
      delete selectedCounts[menuId];
    }
    refreshOrderCounts();
  }

  function refreshOrderCounts() {
    document.querySelectorAll("[data-order-count]").forEach((node) => {
      const menuId = node.dataset.orderCount;
      node.textContent = String(Number(selectedCounts[menuId] || 0));
    });
  }

  function renderHistoryContent(payload) {
    const target = document.getElementById("checkout-summary-content");
    const wrapper = document.getElementById("checkout-summary");
    if (!target || !wrapper) return;

    const orders = Array.isArray(payload.orders) ? payload.orders : [];
    if (!orders.length) {
      target.innerHTML = "<p>まだ注文履歴はありません。</p>";
      wrapper.hidden = false;
      return;
    }

    target.innerHTML = `
      <p class="text-soft">座席: ${payload.seatNo || "-"} / 注文件数: ${payload.orderCount || 0}件</p>
      <div class="section-stack">
        ${orders.map((order) => `
          <article class="panel">
            <p><strong>注文番号:</strong> ${order.orderId} / <strong>状態:</strong> ${order.status}</p>
            <p class="text-soft">${formatDateTime(order.createdAt)}</p>
            <ul class="check-list">
              ${(order.items || []).map((item) => `
                <li>${item.menuName} x ${item.quantity} ＝ ${yen(item.lineTotal)}</li>
              `).join("")}
            </ul>
            <p><strong>小計:</strong> ${yen(order.total)}</p>
          </article>
        `).join("")}
      </div>
      <p class="price">合計金額: ${yen(payload.grandTotal || 0)}</p>
    `;
    wrapper.hidden = false;
  }

  function collectItems() {
    return Object.entries(selectedCounts)
      .filter(([, quantity]) => Number(quantity) > 0)
      .map(([menuId, quantity]) => ({
        menuId,
        quantity: Number(quantity)
      }));
  }

  async function submitOrder(event) {
    event.preventDefault();
    const canOrderNow = sessionInfo && sessionInfo.status === "ACTIVE" && getRemainingSecondsLocal() > 0;
    if (!canOrderNow) {
      setStatus("このセッションでは注文できません。", true);
      return;
    }

    const items = collectItems();
    if (!items.length) {
      setStatus("カードを押して注文商品を選択してください。", true);
      return;
    }

    try {
      const response = await fetch(`${backendBaseUrl}/api/cafe/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=UTF-8" },
        body: JSON.stringify({ sessionToken, items })
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(result.message || "注文送信に失敗しました。");
      }

      setStatus(`注文を受け付けました。注文番号: ${result.orderId}`, false);
      selectedCounts = {};
      refreshOrderCounts();
      const checkout = document.getElementById("checkout-summary");
      if (checkout) checkout.hidden = true;
    } catch (error) {
      setStatus(error.message || "注文送信に失敗しました。", true);
      await refreshSessionStatus();
    }
  }

  async function showCheckoutSummary() {
    try {
      const payload = await fetchOrderHistory();
      renderHistoryContent(payload);
    } catch (error) {
      setStatus(error.message || "お会計情報の取得に失敗しました。", true);
    }
  }

  async function refreshSessionStatus() {
    try {
      sessionInfo = await fetchSessionStatus();
      updateSessionSummary();
    } catch (error) {
      setStatus(error.message || "セッション更新に失敗しました。", true);
      setOrderEnabled(false);
    }
  }

  function startStatusPolling() {
    if (statusTimer) clearInterval(statusTimer);
    statusTimer = setInterval(() => {
      refreshSessionStatus();
    }, 30000);
  }

  function startSessionClock() {
    if (sessionClockTimer) clearInterval(sessionClockTimer);
    sessionClockTimer = setInterval(() => {
      updateSessionSummary();
    }, 1000);
  }

  function getRemainingSecondsLocal() {
    if (!sessionInfo?.expiresAt) return 0;
    const expires = new Date(sessionInfo.expiresAt).getTime();
    if (!Number.isFinite(expires)) return 0;
    const diff = Math.floor((expires - Date.now()) / 1000);
    return Math.max(0, diff);
  }

  async function init() {
    const form = document.getElementById("cafe-order-form");
    if (!form) return;

    sessionToken = getSessionTokenFromQuery();
    if (!sessionToken) {
      setStatus("注文用セッションが見つかりません。レシートQRからアクセスしてください。", true);
      setOrderEnabled(false);
      return;
    }

    try {
      const payload = await fetchOrderMenu();
      sessionInfo = payload.session;
      menuCache = Array.isArray(payload.menus) ? payload.menus : [];
      renderMenus(menuCache);
      updateSessionSummary();
      startStatusPolling();
      startSessionClock();
    } catch (error) {
      setStatus(error.message || "注文ページの初期化に失敗しました。", true);
      setOrderEnabled(false);
      return;
    }

    form.addEventListener("submit", submitOrder);
    document.getElementById("show-checkout-button")?.addEventListener("click", showCheckoutSummary);
  }

  window.addEventListener("beforeunload", () => {
    if (statusTimer) {
      clearInterval(statusTimer);
      statusTimer = null;
    }
    if (sessionClockTimer) {
      clearInterval(sessionClockTimer);
      sessionClockTimer = null;
    }
  });

  document.addEventListener("DOMContentLoaded", init);
})();
