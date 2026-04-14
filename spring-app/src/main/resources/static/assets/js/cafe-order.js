window.CafeOrderPage = (function () {
  const warningThresholdSeconds = 5 * 60;
  const renderBackendBaseUrl = "https://portfolio-hum.onrender.com";
  const backendBaseUrl = (function resolveBackendBaseUrl() {
    const params = new URLSearchParams(window.location.search);
    const apiBase = params.get("apiBase");
    const { hostname, port, origin } = window.location;
    if (apiBase && apiBase.trim()) {
      const normalized = apiBase.trim().replace(/\/+$/, "");
      if (hostname.endsWith("github.io") && /^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/i.test(normalized)) {
        return renderBackendBaseUrl;
      }
      return normalized;
    }
    if (hostname.endsWith("github.io")) return renderBackendBaseUrl;
    if (hostname.endsWith("onrender.com")) return origin;
    if ((hostname === "127.0.0.1" || hostname === "localhost") && port === "3000") {
      return renderBackendBaseUrl;
    }
    return renderBackendBaseUrl;
  })();

  let selectedCounts = {};
  let sessionToken = "";
  let sessionInfo = null;
  let latestGrandTotal = 0;
  let statusTimer = null;
  let clockTimer = null;
  let historyTimer = null;
  let checkoutConfirmAction = null;
  let remainingBaseSeconds = null;
  let remainingCapturedAtMs = null;

  function yen(value) {
    return "¥" + Number(value || 0).toLocaleString("ja-JP");
  }

  function setStatus(text, isError) {
    const el = document.getElementById("cafe-order-status");
    if (!el) return;
    el.textContent = text || "";
    el.style.color = isError ? "#b42318" : "#6d5039";
  }

  function showInfoModal(message) {
    const modal = document.getElementById("cafe-order-modal");
    const msg = document.getElementById("cafe-order-modal-message");
    if (!modal || !msg) return;
    msg.textContent = message || "";
    modal.hidden = false;
  }

  function closeInfoModal() {
    const modal = document.getElementById("cafe-order-modal");
    if (modal) modal.hidden = true;
  }

  function showCheckoutModal(action) {
    const modal = document.getElementById("cafe-checkout-confirm-modal");
    if (!modal) {
      if (typeof action === "function") action();
      return;
    }
    checkoutConfirmAction = action || null;
    modal.hidden = false;
  }

  function closeCheckoutModal() {
    const modal = document.getElementById("cafe-checkout-confirm-modal");
    if (!modal) return;
    modal.hidden = true;
    checkoutConfirmAction = null;
  }

  function getSessionTokenFromQuery() {
    const token = SiteRouter.getQueryParam("session");
    return token ? String(token).trim() : "";
  }

  function captureRemainingBase(info) {
    if (!info) return;
    const remain = Number(info.remainingSeconds);
    if (Number.isFinite(remain)) {
      remainingBaseSeconds = Math.max(0, Math.floor(remain));
      remainingCapturedAtMs = Date.now();
    } else {
      remainingBaseSeconds = null;
      remainingCapturedAtMs = null;
    }
  }

  function formatDateTime(value, epochMs) {
    if (Number.isFinite(Number(epochMs)) && Number(epochMs) > 0) {
      const byEpoch = new Date(Number(epochMs));
      if (!Number.isNaN(byEpoch.getTime())) {
        return byEpoch.toLocaleString("ja-JP", {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit"
        });
      }
    }
    if (!value) return "-";
    let normalized = value;
    if (typeof value === "string") {
      const trimmed = value.trim();
      // Backend sends LocalDateTime without zone on some endpoints.
      // Treat timezone-less ISO-like strings as UTC so JST clients display expected local time.
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?$/.test(trimmed)) {
        normalized = trimmed + "Z";
      }
    }
    const d = new Date(normalized);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString("ja-JP", {
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
    return m + "分" + String(s).padStart(2, "0") + "秒";
  }

  function getRemainingSecondsLocal() {
    if (remainingBaseSeconds != null && remainingCapturedAtMs != null) {
      const elapsed = Math.floor((Date.now() - remainingCapturedAtMs) / 1000);
      return Math.max(0, remainingBaseSeconds - Math.max(0, elapsed));
    }
    if (!sessionInfo || !sessionInfo.expiresAt) return 0;
    const expires = new Date(sessionInfo.expiresAt).getTime();
    if (!Number.isFinite(expires)) return 0;
    return Math.max(0, Math.floor((expires - Date.now()) / 1000));
  }

  function getDisplayExpiresAt() {
    if (remainingBaseSeconds != null && remainingCapturedAtMs != null) {
      return new Date(remainingCapturedAtMs + remainingBaseSeconds * 1000);
    }
    return sessionInfo ? sessionInfo.expiresAt : null;
  }

  function isSessionOrderable() {
    return !!(
      sessionInfo &&
      sessionInfo.status === "ACTIVE" &&
      getRemainingSecondsLocal() > 0
    );
  }

  function setOrderEnabled(enabled) {
    const submit = document.getElementById("bottom-order-button");
    if (submit) submit.disabled = !enabled;

    document.querySelectorAll("[data-order-card]").forEach((card) => {
      card.classList.toggle("is-disabled", !enabled);
    });
    document.querySelectorAll("[data-order-plus],[data-order-minus]").forEach((btn) => {
      btn.disabled = !enabled;
    });
  }

  function setFinishedScreen(visible) {
    const screen = document.getElementById("cafe-finished-screen");
    if (screen) screen.hidden = !visible;
  }

  function updateSessionSummary() {
    const summary = document.getElementById("cafe-session-summary");
    if (!summary) return;

    if (!sessionInfo) {
      summary.textContent = "セッション情報を読み込み中です...";
      setOrderEnabled(false);
      setFinishedScreen(false);
      return;
    }

    const remain = getRemainingSecondsLocal();
    const baseSummary =
      "座席: " + sessionInfo.seatNo +
      " / 有効期限: " + formatDateTime(getDisplayExpiresAt()) +
      " / 残り: " + formatRemaining(remain);
    summary.textContent =
      "座席: " + sessionInfo.seatNo +
      " / 有効期限: " + formatDateTime(getDisplayExpiresAt()) +
      " / 残り: " + formatRemaining(remain) + "まで注文できます";

    if (sessionInfo.status === "CHECKED_OUT") {
      summary.textContent = baseSummary + " / 会計済み";
      setOrderEnabled(false);
      setStatus("", false);
      const finishedTotal = document.getElementById("cafe-finished-total");
      if (finishedTotal) finishedTotal.textContent = "合計金額: " + yen(latestGrandTotal);
      setFinishedScreen(true);
      return;
    }

    if (sessionInfo.status === "EXPIRED" || remain <= 0) {
      summary.textContent = baseSummary + " / 注文受付終了";
      setOrderEnabled(false);
      setStatus("注文可能時間が終了しました。", true);
      setFinishedScreen(false);
      return;
    }

    summary.textContent = baseSummary + "まで注文できます";
    if (remain <= warningThresholdSeconds) {
      setStatus("ラストオーダー5分前です。", true);
    } else {
      setStatus("", false);
    }
    setOrderEnabled(true);
    setFinishedScreen(false);
  }

  async function fetchJson(url, options) {
    let res;
    try {
      res = await fetch(url, options);
    } catch (error) {
      throw new Error("通信に失敗しました。ネットワーク接続かAPI接続先を確認してください。(" + backendBaseUrl + ")");
    }
    const payload = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(payload.message || ("通信に失敗しました (" + res.status + ")"));
    }
    return payload;
  }

  function renderMenuCard(item) {
    const imageUrl = item.image ? (SiteRouter.getBasePath() + "image/" + item.image) : "";
    const media = imageUrl
      ? '<figure class="media-photo"><img src="' + imageUrl + '" alt="' + item.name + '"></figure>'
      : '<div class="media-placeholder"></div>';

    return (
      '<article class="menu-card cafe-order-card" data-order-card="' + item.id + '" role="button" tabindex="0" aria-label="' + item.name + 'を追加">' +
      media +
      '<div class="cafe-order-card__body">' +
      '<div class="cafe-order-card__row">' +
      "<h3>" + item.name + "</h3>" +
      '<div class="cafe-order-card__counter">' +
      '<button class="cafe-order-card__step" type="button" data-order-minus="' + item.id + '" aria-label="' + item.name + 'を1つ減らす">-</button>' +
      '<span class="cafe-order-card__count" data-order-count="' + item.id + '">0</span>' +
      '<button class="cafe-order-card__step" type="button" data-order-plus="' + item.id + '" aria-label="' + item.name + 'を1つ増やす">+</button>' +
      "</div></div>" +
      '<p class="price">' + yen(item.price) + "</p>" +
      "</div></article>"
    );
  }

  function refreshOrderCounts() {
    document.querySelectorAll("[data-order-count]").forEach((node) => {
      const menuId = node.dataset.orderCount;
      node.textContent = String(Number(selectedCounts[menuId] || 0));
    });
  }

  function increment(menuId) {
    if (!isSessionOrderable()) {
      if (sessionInfo && sessionInfo.status === "CHECKED_OUT") {
        showInfoModal("お会計が確定しています。");
      }
      return;
    }
    selectedCounts[menuId] = Number(selectedCounts[menuId] || 0) + 1;
    refreshOrderCounts();
  }

  function decrement(menuId) {
    if (!isSessionOrderable()) return;
    const current = Number(selectedCounts[menuId] || 0);
    if (current <= 0) return;
    selectedCounts[menuId] = current - 1;
    if (selectedCounts[menuId] <= 0) delete selectedCounts[menuId];
    refreshOrderCounts();
  }

  function bindCardEvents() {
    document.querySelectorAll("[data-order-card]").forEach((card) => {
      card.addEventListener("click", () => increment(card.dataset.orderCard));
      card.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          increment(card.dataset.orderCard);
        }
      });
    });

    document.querySelectorAll("[data-order-plus]").forEach((btn) => {
      btn.addEventListener("click", (event) => {
        event.stopPropagation();
        increment(btn.dataset.orderPlus);
      });
    });

    document.querySelectorAll("[data-order-minus]").forEach((btn) => {
      btn.addEventListener("click", (event) => {
        event.stopPropagation();
        decrement(btn.dataset.orderMinus);
      });
    });
  }

  function renderMenus(menus) {
    const drinkTarget = document.getElementById("cafe-order-menu-drink");
    const foodTarget = document.getElementById("cafe-order-menu-food");
    if (!drinkTarget || !foodTarget) return;

    const drinks = menus.filter((item) => String(item.category || item.type || "").toUpperCase() === "DRINK");
    const foods = menus.filter((item) => {
      const c = String(item.category || item.type || "").toUpperCase();
      return c === "FOOD" || c === "TREAT";
    });

    drinkTarget.innerHTML = drinks.length
      ? drinks.map(renderMenuCard).join("")
      : "<p>ドリンクメニューはありません。</p>";

    foodTarget.innerHTML = foods.length
      ? foods.map(renderMenuCard).join("")
      : "<p>フードメニューはありません。</p>";

    bindCardEvents();
    refreshOrderCounts();
  }

  function renderHistory(payload) {
    const target = document.getElementById("checkout-summary-content");
    if (!target) return;

    const orders = Array.isArray(payload.orders) ? payload.orders : [];
    latestGrandTotal = Number(payload.grandTotal || 0);

    if (!orders.length) {
      target.innerHTML = '<p>注文履歴はまだありません。</p><p class="price">合計金額: ' + yen(0) + "</p>";
      return;
    }

    const body = orders.map((order) => {
      const cls = order.status === "SERVED" ? "panel cafe-order-history-item--served" : "panel";
      const lines = (order.items || [])
        .map((item) => "<li>" + item.menuName + " ×" + item.quantity + " / " + yen(item.lineTotal) + "</li>")
        .join("");
      return (
        '<article class="' + cls + '">' +
        '<p class="text-soft">' + formatDateTime(order.createdAt, order.createdAtEpochMs) + "</p>" +
        '<ul class="check-list">' + lines + "</ul>" +
        "</article>"
      );
    }).join("");

    target.innerHTML =
      '<p class="text-soft">座席: ' + (payload.seatNo || "-") + " / 注文件数: " + (payload.orderCount || 0) + "件</p>" +
      '<div class="section-stack">' + body + "</div>" +
      '<p class="price">合計金額: ' + yen(payload.grandTotal || 0) + "</p>";
  }

  function collectItems() {
    return Object.entries(selectedCounts)
      .filter(([, qty]) => Number(qty) > 0)
      .map(([menuId, qty]) => ({ menuId, quantity: Number(qty) }));
  }

  async function refreshHistory() {
    if (!sessionToken) return;
    const payload = await fetchJson(backendBaseUrl + "/api/cafe/orders/history?session=" + encodeURIComponent(sessionToken));
    renderHistory(payload);
  }

  async function checkoutSession() {
    if (!sessionToken) return;
    showCheckoutModal(async function () {
      try {
        const result = await fetchJson(
          backendBaseUrl + "/api/cafe/sessions/" + encodeURIComponent(sessionToken) + "/checkout",
          { method: "POST", headers: { "Content-Type": "application/json; charset=UTF-8" } }
        );
        sessionInfo = result.session || sessionInfo;
        captureRemainingBase(sessionInfo);
        updateSessionSummary();
        await refreshHistory();
      } catch (error) {
        setStatus(error.message || "お会計の確定に失敗しました。", true);
      }
    });
  }

  async function submitOrder(event) {
    event.preventDefault();

    if (sessionInfo && sessionInfo.status === "CHECKED_OUT") {
      showInfoModal("お会計が確定しています。");
      return;
    }
    if (!isSessionOrderable()) {
      setStatus("現在このセッションでは注文できません。", true);
      return;
    }

    const items = collectItems();
    if (!items.length) {
      setStatus("注文する商品を選択してください。", true);
      return;
    }

    try {
      await fetchJson(backendBaseUrl + "/api/cafe/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=UTF-8" },
        body: JSON.stringify({ sessionToken, items })
      });
      selectedCounts = {};
      refreshOrderCounts();
      setStatus("注文を受け付けました。", false);
      await refreshHistory();
      await refreshSessionStatus();
    } catch (error) {
      setStatus(error.message || "注文に失敗しました。", true);
    }
  }

  async function refreshSessionStatus() {
    try {
      sessionInfo = await fetchJson(backendBaseUrl + "/api/cafe/sessions/" + encodeURIComponent(sessionToken));
      captureRemainingBase(sessionInfo);
      updateSessionSummary();
    } catch (error) {
      setOrderEnabled(false);
      setStatus(error.message || "セッション情報の取得に失敗しました。", true);
    }
  }

  function startTimers() {
    if (statusTimer) clearInterval(statusTimer);
    if (clockTimer) clearInterval(clockTimer);
    if (historyTimer) clearInterval(historyTimer);

    statusTimer = setInterval(refreshSessionStatus, 30000);
    clockTimer = setInterval(updateSessionSummary, 1000);
    historyTimer = setInterval(() => {
      refreshHistory().catch(() => {});
    }, 10000);
  }

  async function init() {
    document.getElementById("cafe-order-modal-ok")?.addEventListener("click", closeInfoModal);
    document.querySelector("[data-order-modal-close]")?.addEventListener("click", closeInfoModal);
    document.getElementById("cafe-checkout-modal-cancel")?.addEventListener("click", closeCheckoutModal);
    document.querySelector("[data-checkout-modal-close]")?.addEventListener("click", closeCheckoutModal);
    document.getElementById("cafe-checkout-modal-ok")?.addEventListener("click", async function () {
      const action = checkoutConfirmAction;
      closeCheckoutModal();
      if (typeof action === "function") {
        await action();
      }
    });

    const form = document.getElementById("cafe-order-form");
    if (!form) return;
    form.addEventListener("submit", submitOrder);
    document.getElementById("show-checkout-button")?.addEventListener("click", checkoutSession);

    sessionToken = getSessionTokenFromQuery();
    if (!sessionToken) {
      setOrderEnabled(false);
      setStatus("セッショントークンがありません。", true);
      return;
    }

    try {
      const payload = await fetchJson(
        backendBaseUrl + "/api/cafe/order-menu?session=" + encodeURIComponent(sessionToken)
      );
      sessionInfo = payload.session;
      captureRemainingBase(sessionInfo);
      renderMenus(Array.isArray(payload.menus) ? payload.menus : []);
      await refreshHistory();
      updateSessionSummary();
      startTimers();
    } catch (error) {
      setOrderEnabled(false);
      setStatus(error.message || "注文ページの読み込みに失敗しました。", true);
    }
  }

  window.addEventListener("beforeunload", () => {
    if (statusTimer) clearInterval(statusTimer);
    if (clockTimer) clearInterval(clockTimer);
    if (historyTimer) clearInterval(historyTimer);
  });

  document.addEventListener("DOMContentLoaded", init);
})();
