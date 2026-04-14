window.ProductsPage = (function () {
  let productsCache = [];
  let stocksCache = {};
  let recommendedStocksCache = {};
  const backendBaseUrl = (() => {
    const { hostname, port, origin } = window.location;
    if (hostname.endsWith("github.io")) return "https://portfolio-hum.onrender.com";
    if (hostname.endsWith("onrender.com")) return origin;
    if ((hostname === "127.0.0.1" || hostname === "localhost") && port === "3000") return "http://localhost:8080";
    return origin;
  })();

  function categoryLabel(category) {
    const labels = {
      cage: "ケージ",
      food: "フード",
      toy: "おもちゃ",
      care: "ケア用品",
      starter: "スターターセット"
    };
    return labels[category] || category;
  }

  function badgeLabel(badge) {
    const labels = {
      popular: "人気",
      new: "新着",
      recommended: "おすすめ",
      standard: "定番",
      seasonal: "季節限定",
      "人気": "人気",
      "新着": "新着",
      "おすすめ": "おすすめ",
      "定番": "定番",
      "季節限定": "季節限定"
    };
    return labels[badge] || badge;
  }

  function getCartQuantity(productId) {
    const cart = JSON.parse(localStorage.getItem("hamu_cart") || "[]");
    const item = cart.find((entry) => entry.id === productId);
    return item ? item.quantity : 0;
  }

  async function getProducts() {
    if (productsCache.length) return productsCache;
    productsCache = await SiteRouter.fetchJSON("products.json");
    return productsCache;
  }

  async function getStocks(products) {
    if (Object.keys(stocksCache).length > 0) return stocksCache;

    const fallbackDefault = 30;
    const fallback = {};
    products.forEach((product) => {
      fallback[product.id] = fallbackDefault;
    });

    try {
      const response = await fetch(`${backendBaseUrl}/api/products/stocks`);
      if (!response.ok) {
        stocksCache = fallback;
        return stocksCache;
      }

      const payload = await response.json();
      const apiStocks = payload?.stocks || {};
      const apiDefaultStock = Number(payload?.defaultStock ?? fallbackDefault);
      const apiRecommendedStocks = payload?.recommendedStocks || {};
      const resolved = {};
      const resolvedRecommended = {};

      products.forEach((product) => {
        resolved[product.id] = Number(apiStocks[product.id] ?? apiDefaultStock);
        resolvedRecommended[product.id] = Number(apiRecommendedStocks[product.id] ?? apiDefaultStock);
      });

      stocksCache = resolved;
      recommendedStocksCache = resolvedRecommended;
      return stocksCache;
    } catch (error) {
      stocksCache = fallback;
      recommendedStocksCache = { ...fallback };
      return stocksCache;
    }
  }

  function getStock(productId) {
    return Number(stocksCache[productId] ?? 0);
  }

  function getRemainingStock(productId) {
    return Math.max(0, getStock(productId) - getCartQuantity(productId));
  }

  function getRecommendedStock(productId) {
    return Number(recommendedStocksCache[productId] ?? 30);
  }

  function productMedia(product) {
    return product.image
      ? `<figure class="media-photo"><img src="${SiteRouter.getBasePath()}image/${product.image}" alt="${product.name}"></figure>`
      : `<div class="product-card__media"></div>`;
  }

  function quantityBadge(productId) {
    const quantity = getCartQuantity(productId);
    return `
      <div class="cart-quantity-control${quantity > 0 ? " is-visible" : ""}" data-inline-cart-control="${productId}">
        <button class="quantity-stepper" type="button" data-qty-minus="${productId}" aria-label="数量を1つ減らす">-</button>
        <span class="cart-inline-count" data-inline-cart-count="${productId}" aria-live="polite">${quantity}</span>
        <button class="quantity-stepper" type="button" data-qty-plus="${productId}" aria-label="数量を1つ増やす">+</button>
      </div>
    `;
  }

  function stockLine(productId) {
    return `<p class="text-soft" data-stock-label="${productId}">在庫あり</p>`;
  }

  function cardTemplate(product, extraClass = "") {
    return `
      <article class="product-card${extraClass ? ` ${extraClass}` : ""}">
        ${productMedia(product)}
        <div class="product-card__meta">
          <span class="chip">${categoryLabel(product.category)}</span>
          ${product.badge ? `<span class="status-chip">${badgeLabel(product.badge)}</span>` : ""}
        </div>
        <h3>${product.name}</h3>
        <p class="text-soft">${product.shortDescription}</p>
        ${stockLine(product.id)}
        <p class="price">${SiteRouter.formatPrice(product.price)}</p>
        <div class="button-row">
          <a class="button button--ghost" href="${SiteRouter.getBasePath()}products/detail.html?id=${product.id}">詳細を見る</a>
          <button class="button" type="button" data-add-product="${product.id}">カートに追加</button>
          ${quantityBadge(product.id)}
        </div>
      </article>
    `;
  }

  function featuredProductSlide(product) {
    return `
      <article class="product-card">
        ${productMedia(product)}
        <div class="product-card__meta">
          <span class="chip">${categoryLabel(product.category)}</span>
          ${product.badge ? `<span class="status-chip">${badgeLabel(product.badge)}</span>` : ""}
        </div>
        <h3>${product.name}</h3>
        <p class="text-soft">${product.shortDescription}</p>
        ${stockLine(product.id)}
        <p class="price">${SiteRouter.formatPrice(product.price)}</p>
        <div class="button-row">
          <a class="button button--ghost" href="${SiteRouter.getBasePath()}products/detail.html?id=${product.id}">詳細を見る</a>
          <button class="button" type="button" data-add-product="${product.id}">カートに追加</button>
          ${quantityBadge(product.id)}
        </div>
      </article>
    `;
  }

  function chunkItems(items, size) {
    const chunks = [];
    for (let index = 0; index < items.length; index += size) {
      chunks.push(items.slice(index, index + size));
    }
    return chunks;
  }

  function refreshStockLabels() {
    document.querySelectorAll("[data-stock-label]").forEach((node) => {
      const productId = node.dataset.stockLabel;
      const remaining = getRemainingStock(productId);
      const recommended = getRecommendedStock(productId);
      const threshold = Math.max(1, Math.floor(recommended * 0.15));

      if (remaining <= 0) {
        node.textContent = "売り切れ";
        return;
      }

      if (remaining < threshold) {
        node.textContent = "残りわずか";
        return;
      }

      node.textContent = "在庫あり";
    });
  }

  function refreshAddButtons() {
    document.querySelectorAll("[data-add-product]").forEach((button) => {
      const remaining = getRemainingStock(button.dataset.addProduct);
      button.disabled = remaining <= 0;
      button.textContent = remaining <= 0 ? "売り切れ" : "カートに追加";
    });
  }

  function refreshInlineCounts() {
    document.querySelectorAll("[data-inline-cart-count]").forEach((node) => {
      const quantity = getCartQuantity(node.dataset.inlineCartCount);
      node.textContent = String(quantity);
    });

    document.querySelectorAll("[data-inline-cart-control]").forEach((node) => {
      const quantity = getCartQuantity(node.dataset.inlineCartControl);
      node.classList.toggle("is-visible", quantity > 0);
    });

    refreshStockLabels();
    refreshAddButtons();
  }

  function changeQuantity(productId, diff) {
    const quantity = getCartQuantity(productId);

    if (diff > 0) {
      if (quantity === 0) return false;
      if (getRemainingStock(productId) <= 0) {
        alert("この商品の在庫がありません。");
        return false;
      }
      Cart.updateQuantity(productId, quantity + diff);
      return true;
    }

    if (quantity <= 1) {
      Cart.removeItem(productId);
      return true;
    }

    Cart.updateQuantity(productId, quantity + diff);
    return true;
  }

  function bindQuantityControls() {
    document.querySelectorAll("[data-qty-plus]").forEach((button) => {
      button.addEventListener("click", () => {
        changeQuantity(button.dataset.qtyPlus, 1);
        refreshInlineCounts();
      });
    });

    document.querySelectorAll("[data-qty-minus]").forEach((button) => {
      button.addEventListener("click", () => {
        changeQuantity(button.dataset.qtyMinus, -1);
        refreshInlineCounts();
      });
    });
  }

  function bindAddButtons(products) {
    document.querySelectorAll("[data-add-product]").forEach((button) => {
      button.addEventListener("click", () => {
        const product = products.find((item) => item.id === button.dataset.addProduct);
        if (!product) return;
        if (getRemainingStock(product.id) <= 0) {
          alert("在庫切れのため追加できません。");
          return;
        }

        Cart.addItem({
          id: product.id,
          name: product.name,
          price: product.price,
          categoryLabel: categoryLabel(product.category)
        });

        refreshInlineCounts();
      });
    });
  }

  function setupHomeCarousel(target) {
    const track = target.querySelector(".hamster-carousel__track");
    const slides = Array.from(target.querySelectorAll(".hamster-carousel__slide"));
    const prev = target.querySelector("[data-carousel-prev]");
    const next = target.querySelector("[data-carousel-next]");
    const dotsWrap = target.querySelector(".hamster-carousel__dots");
    if (!track || !slides.length || !dotsWrap) return;

    let currentPage = 0;
    const pageCount = slides.length;

    dotsWrap.innerHTML = Array.from({ length: pageCount }, (_, index) => {
      return `<button type="button" class="hamster-carousel__dot" data-carousel-dot="${index}" aria-label="${index + 1}ページ目へ移動"></button>`;
    }).join("");

    function updateCarousel(page) {
      currentPage = (page + pageCount) % pageCount;
      track.style.transform = `translateX(-${currentPage * 100}%)`;
      Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot, index) => {
        dot.classList.toggle("is-active", index === currentPage);
      });
    }

    prev?.addEventListener("click", () => updateCarousel(currentPage - 1));
    next?.addEventListener("click", () => updateCarousel(currentPage + 1));
    Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot) => {
      dot.addEventListener("click", () => updateCarousel(Number(dot.dataset.carouselDot)));
    });

    updateCarousel(0);
  }

  function renderCarouselList(target, products) {
    if (!target) return;
    const pages = chunkItems(products, 3);

    target.innerHTML = `
      <div class="hamster-carousel">
        <div class="hamster-carousel__viewport">
          <div class="hamster-carousel__track">
            ${pages.map((group) => `
              <div class="hamster-carousel__slide">
                <div class="hamster-carousel__page">
                  ${group.map(featuredProductSlide).join("")}
                </div>
              </div>
            `).join("")}
          </div>
        </div>
        <div class="hamster-carousel__controls">
          <button class="button button--ghost" type="button" data-carousel-prev>前へ</button>
          <div class="hamster-carousel__dots"></div>
          <button class="button button--ghost" type="button" data-carousel-next>次へ</button>
        </div>
      </div>
    `;

    setupHomeCarousel(target);
  }

  async function renderHomeSections() {
    const products = await getProducts();
    await getStocks(products);

    renderCarouselList(document.getElementById("starter-products"), products.filter((item) => item.category === "starter"));
    renderCarouselList(document.getElementById("popular-products"), products.filter((item) => item.popular).slice(0, 6));

    bindAddButtons(products);
    bindQuantityControls();
    refreshInlineCounts();
  }

  async function renderProductLanding() {
    const target = document.getElementById("product-category-preview");
    if (!target) return;

    const products = await getProducts();
    target.innerHTML = ["cage", "food", "toy", "care", "starter"].map((category) => {
      const first = products.find((item) => item.category === category);
      return `
        <article class="info-card">
          <h3>${categoryLabel(category)}</h3>
          <p>${first ? first.shortDescription : ""}</p>
          <a class="button button--ghost" href="index.html?category=${category}">一覧を見る</a>
        </article>
      `;
    }).join("");
  }

  async function renderProductListPage() {
    const target = document.getElementById("product-list");
    if (!target) return;

    const products = await getProducts();
    await getStocks(products);

    const filter = SiteRouter.getQueryParam("category");
    const filtered = filter ? products.filter((item) => item.category === filter) : products;

    target.innerHTML = filtered.map(cardTemplate).join("");
    bindAddButtons(products);
    bindQuantityControls();
    refreshInlineCounts();

    document.querySelectorAll("[data-filter-category]").forEach((button) => {
      if (button.dataset.filterCategory === filter || (!filter && button.dataset.filterCategory === "all")) {
        button.classList.add("is-active");
      }

      button.addEventListener("click", () => {
        const next = button.dataset.filterCategory;
        window.location.href = next === "all" ? "index.html" : `index.html?category=${next}`;
      });
    });
  }

  async function renderProductDetailPage() {
    const target = document.getElementById("product-detail");
    if (!target) return;

    const products = await getProducts();
    await getStocks(products);

    const id = SiteRouter.getQueryParam("id") || products[0]?.id;
    const product = products.find((item) => item.id === id);

    if (!product) {
      target.innerHTML = '<div class="empty-state">商品が見つかりませんでした。</div>';
      return;
    }

    target.innerHTML = `
      <div class="detail-layout">
        ${product.image
          ? `<figure class="detail-card media-photo"><img src="${SiteRouter.getBasePath()}image/${product.image}" alt="${product.name}"></figure>`
          : `<div class="detail-card detail-card__media"></div>`}
        <article class="detail-card">
          <p class="eyebrow">${categoryLabel(product.category)}</p>
          <h1>${product.name}</h1>
          <p class="text-soft">${product.description}</p>
          ${stockLine(product.id)}
          <p class="price">${SiteRouter.formatPrice(product.price)}</p>
          <ul class="check-list">${product.features.map((feature) => `<li>${feature}</li>`).join("")}</ul>
          <div class="button-row">
            <button class="button" type="button" id="detail-add-cart" data-add-product="${product.id}">カートに追加</button>
            ${quantityBadge(product.id)}
            <a class="button button--ghost" href="index.html?category=${product.category}">一覧に戻る</a>
          </div>
        </article>
      </div>
    `;

    document.getElementById("detail-add-cart")?.addEventListener("click", () => {
      if (getRemainingStock(product.id) <= 0) {
        alert("在庫切れのため追加できません。");
        return;
      }
      Cart.addItem({
        id: product.id,
        name: product.name,
        price: product.price,
        categoryLabel: categoryLabel(product.category)
      });
      refreshInlineCounts();
    });

    bindQuantityControls();
    refreshInlineCounts();
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderHomeSections();
    renderProductLanding();
    renderProductListPage();
    renderProductDetailPage();
    document.addEventListener("cart:updated", refreshInlineCounts);
  });
})();
