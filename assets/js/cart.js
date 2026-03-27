window.Cart = (function () {
  const storageKey = "hamu_cart";
  const memberKey = "hamu_member";
  const checkoutProfileKey = "hamu_checkout_profile";
  const lastOrderTotalKey = "hamu_last_order_total";
  const lastOrderPointsKey = "hamu_last_order_points";
  const backendBaseUrl = "http://localhost:8080";

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

  function getMember() {
    try {
      return JSON.parse(localStorage.getItem(memberKey) || "null");
    } catch (error) {
      return null;
    }
  }

  function saveMember(member) {
    localStorage.setItem(memberKey, JSON.stringify(member));
    document.dispatchEvent(new Event("member:updated"));
  }

  function getCheckoutProfileKey() {
    const member = getMember();
    const email = member?.loggedIn ? String(member.email || "").trim().toLowerCase() : "";
    return email ? `${checkoutProfileKey}_${email}` : checkoutProfileKey;
  }

  function getCheckoutProfile() {
    try {
      return JSON.parse(localStorage.getItem(getCheckoutProfileKey()) || "null") || {};
    } catch (error) {
      return {};
    }
  }

  function saveCheckoutProfile(profile) {
    localStorage.setItem(getCheckoutProfileKey(), JSON.stringify(profile));
  }

  function clearCheckoutProfile() {
    localStorage.removeItem(getCheckoutProfileKey());
  }

  async function syncPointsToBackend(email, earnedPoints) {
    const response = await fetch(`${backendBaseUrl}/api/members/points`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=UTF-8"
      },
      body: JSON.stringify({
        email,
        delta: earnedPoints
      })
    });

    const result = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(result.message || "ポイントの反映に失敗しました。");
    }

    return result;
  }

  async function purchaseStock(items) {
    const response = await fetch(`${backendBaseUrl}/api/products/purchase`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=UTF-8"
      },
      body: JSON.stringify({
        items: items.map((item) => ({
          productId: item.id,
          quantity: Number(item.quantity || 0)
        }))
      })
    });

    const result = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(result.message || "在庫更新に失敗しました。");
    }

    return result;
  }

  async function awardPoints(total) {
    const member = getMember();
    if (!member?.loggedIn) {
      localStorage.setItem(lastOrderPointsKey, "0");
      return 0;
    }

    const earnedPoints = Math.floor(Number(total || 0) / 100);
    if (earnedPoints <= 0) {
      localStorage.setItem(lastOrderPointsKey, "0");
      return 0;
    }

    const result = await syncPointsToBackend(member.email, earnedPoints);

    saveMember({
      ...member,
      points: Number(result.points || 0)
    });

    localStorage.setItem(lastOrderPointsKey, String(earnedPoints));
    return earnedPoints;
  }

  function renderCartPage() {
    const list = document.getElementById("cart-items");
    const summary = document.getElementById("cart-summary");
    if (!list || !summary) return;

    const data = getSummary();
    if (!data.items.length) {
      list.innerHTML = '<div class="empty-state">カートに商品が入っていません。商品一覧から追加してください。</div>';
      summary.innerHTML = '<div class="summary-card"><h2>ご購入前に</h2><p>商品を追加すると、ここに合計金額が表示されます。</p><a class="button button--ghost" href="index.html">商品一覧へ</a></div>';
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
          <label>数量<input type="number" min="1" value="${item.quantity}" data-cart-qty="${item.id}"></label>
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

  function normalizePostalCode(value) {
    return String(value || "").replace(/\D/g, "");
  }

  function normalizePhoneNumber(value) {
    return String(value || "").replace(/\D/g, "");
  }

  function collectCheckoutProfile() {
    return {
      name: document.getElementById("checkout-name")?.value || "",
      postalCode: document.getElementById("postal-code")?.value || "",
      address: document.getElementById("address")?.value || "",
      phoneNumber: document.getElementById("phone-number")?.value || "",
      paymentMethod: document.getElementById("payment-method")?.value || "credit-card"
    };
  }

  function fillCheckoutProfile() {
    const profile = getCheckoutProfile();
    const member = getMember();

    const nameInput = document.getElementById("checkout-name");
    const postalInput = document.getElementById("postal-code");
    const addressInput = document.getElementById("address");
    const phoneInput = document.getElementById("phone-number");
    const paymentSelect = document.getElementById("payment-method");

    if (nameInput) {
      nameInput.value = profile.name || member?.name || "";
    }
    if (postalInput) {
      postalInput.value = profile.postalCode || "";
    }
    if (addressInput) {
      addressInput.value = profile.address || "";
    }
    if (phoneInput) {
      phoneInput.value = profile.phoneNumber || "";
    }
    if (paymentSelect && profile.paymentMethod) {
      paymentSelect.value = profile.paymentMethod;
    }
  }

  function resetCheckoutForm() {
    document.getElementById("checkout-name").value = "";
    document.getElementById("postal-code").value = "";
    document.getElementById("address").value = "";
    document.getElementById("phone-number").value = "";
    document.getElementById("payment-method").value = "credit-card";
    const feedback = document.getElementById("postal-feedback");
    if (feedback) {
      feedback.textContent = "郵便番号を入力すると住所を自動で検索できます。";
    }
  }

  function bindCheckoutProfilePersistence() {
    const fields = [
      document.getElementById("checkout-name"),
      document.getElementById("postal-code"),
      document.getElementById("address"),
      document.getElementById("phone-number"),
      document.getElementById("payment-method")
    ].filter(Boolean);

    fields.forEach((field) => {
      field.addEventListener("input", () => {
        saveCheckoutProfile(collectCheckoutProfile());
      });
      field.addEventListener("change", () => {
        saveCheckoutProfile(collectCheckoutProfile());
      });
    });

    document.getElementById("clear-checkout-profile")?.addEventListener("click", () => {
      clearCheckoutProfile();
      resetCheckoutForm();
    });
  }

  async function lookupAddress(postalCode, addressInput, feedback) {
    const normalized = normalizePostalCode(postalCode);
    if (normalized.length !== 7) {
      feedback.textContent = "郵便番号は7桁で入力してください。";
      return;
    }

    feedback.textContent = "住所を検索しています...";

    try {
      const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${normalized}`);
      const data = await response.json();
      const result = data.results?.[0];

      if (!result) {
        feedback.textContent = "該当する住所が見つかりませんでした。";
        return;
      }

      addressInput.value = `${result.address1}${result.address2}${result.address3}`;
      saveCheckoutProfile(collectCheckoutProfile());
      feedback.textContent = "住所を自動入力しました。必要に応じて建物名を入力してください。";
    } catch (error) {
      feedback.textContent = "住所検索に失敗しました。時間をおいて再度お試しください。";
    }
  }

  function renderCheckoutPage() {
    const summary = document.getElementById("checkout-summary");
    const form = document.getElementById("checkout-form");
    if (!summary || !form) return;

    const data = getSummary();
    const expectedPoints = Math.floor(data.total / 100);
    summary.innerHTML = `
      <div class="summary-card">
        <h2>ご注文内容</h2>
        <p>商品点数 <strong>${data.items.length}</strong></p>
        <p>小計 <strong>${SiteRouter.formatPrice(data.subtotal)}</strong></p>
        <p>送料 <strong>${SiteRouter.formatPrice(data.shipping)}</strong></p>
        <p>合計 <strong class="price">${SiteRouter.formatPrice(data.total)}</strong></p>
        <p>今回付与ポイント <strong>${expectedPoints} pt</strong></p>
      </div>
    `;

    fillCheckoutProfile();
    bindCheckoutProfilePersistence();

    const postalInput = document.getElementById("postal-code");
    const addressInput = document.getElementById("address");
    const phoneInput = document.getElementById("phone-number");
    const feedback = document.getElementById("postal-feedback");
    const searchButton = document.getElementById("postal-search");

    if (postalInput && addressInput && feedback) {
      searchButton?.addEventListener("click", () => {
        lookupAddress(postalInput.value, addressInput, feedback);
      });

      postalInput.addEventListener("blur", () => {
        const normalized = normalizePostalCode(postalInput.value);
        if (normalized.length === 7) {
          lookupAddress(normalized, addressInput, feedback);
        }
      });
    }

    if (phoneInput) {
      phoneInput.addEventListener("input", () => {
        phoneInput.value = normalizePhoneNumber(phoneInput.value);
        phoneInput.setCustomValidity("");
      });
    }

    form.addEventListener("submit", async (event) => {
      event.preventDefault();

      if (phoneInput) {
        const normalizedPhone = normalizePhoneNumber(phoneInput.value);
        const isValidPhone = /^\d{10,11}$/.test(normalizedPhone);

        phoneInput.value = normalizedPhone;

        if (!isValidPhone) {
          phoneInput.setCustomValidity("電話番号は数字10桁または11桁で入力してください。");
          phoneInput.reportValidity();
          return;
        }

        phoneInput.setCustomValidity("");
      }

      try {
        saveCheckoutProfile(collectCheckoutProfile());
        localStorage.setItem(lastOrderTotalKey, String(data.total));

        await purchaseStock(data.items);

        try {
          await awardPoints(data.total);
        } catch (pointError) {
          localStorage.setItem(lastOrderPointsKey, "0");
        }

        clear();
        const completePath = document.body.dataset.completePath || "complete.html";
        window.location.href = completePath;
      } catch (error) {
        alert(error.message || "購入処理に失敗しました。");
      }
    });
  }

  function renderCompletePage() {
    const total = document.getElementById("complete-total");
    if (total) {
      const value = Number(localStorage.getItem(lastOrderTotalKey) || 0);
      total.textContent = SiteRouter.formatPrice(value);
    }

    const points = document.getElementById("complete-points");
    if (points) {
      const value = Number(localStorage.getItem(lastOrderPointsKey) || 0);
      points.textContent = `${value} pt`;
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderCartPage();
    renderCheckoutPage();
    renderCompletePage();
  });

  return { addItem, updateQuantity, removeItem, getCart, getSummary, clear };
})();
