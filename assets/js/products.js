window.ProductsPage = (function () {
  let productsCache = [];

  function categoryLabel(category) {
    const labels = {
      cage: "ケージ",
      food: "餌",
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
      人気: "人気",
      新着: "新着",
      おすすめ: "おすすめ",
      定番: "定番",
      季節限定: "季節限定"
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
      <article class="hamster-carousel__slide">
        ${productMedia(product)}
        <div class="product-card__meta">
          <span class="chip">${categoryLabel(product.category)}</span>
          ${product.badge ? `<span class="status-chip">${badgeLabel(product.badge)}</span>` : ""}
        </div>
        <h3>${product.name}</h3>
        <p class="text-soft">${product.shortDescription}</p>
        <p class="price">${SiteRouter.formatPrice(product.price)}</p>
        <div class="button-row">
          <a class="button button--ghost" href="${SiteRouter.getBasePath()}products/detail.html?id=${product.id}">詳細を見る</a>
          <button class="button" type="button" data-add-product="${product.id}">カートに追加</button>
          ${quantityBadge(product.id)}
        </div>
      </article>
    `;
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
  }

  function changeQuantity(productId, diff) {
    const quantity = getCartQuantity(productId);

    if (diff > 0) {
      if (quantity === 0) {
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

  function renderList(target, products) {
    if (target) target.innerHTML = products.map(cardTemplate).join("");
  }

  function setupHomeCarousel(target) {
    const track = target.querySelector(".hamster-carousel__track");
    const slides = Array.from(target.querySelectorAll(".hamster-carousel__slide"));
    const prev = target.querySelector("[data-carousel-prev]");
    const next = target.querySelector("[data-carousel-next]");
    const dotsWrap = target.querySelector(".hamster-carousel__dots");
    if (!track || !slides.length || !dotsWrap) return;

    let currentPage = 0;

    function getSlidesPerView() {
      return window.innerWidth >= 900 ? 3 : 1;
    }

    function getPageCount() {
      return Math.ceil(slides.length / getSlidesPerView());
    }

    function renderDots() {
      const pageCount = getPageCount();
      dotsWrap.innerHTML = Array.from({ length: pageCount }, (_, index) => {
        return `<button type="button" class="hamster-carousel__dot" data-carousel-dot="${index}" aria-label="${index + 1}ページ目へ移動"></button>`;
      }).join("");

      Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot) => {
        dot.addEventListener("click", () => updateCarousel(Number(dot.dataset.carouselDot)));
      });
    }

    function updateCarousel(page) {
      const pageCount = getPageCount();
      if (!pageCount) return;

      currentPage = (page + pageCount) % pageCount;
      track.style.transform = `translateX(-${currentPage * 100}%)`;

      Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot, index) => {
        dot.classList.toggle("is-active", index === currentPage);
      });
    }

    prev?.addEventListener("click", () => updateCarousel(currentPage - 1));
    next?.addEventListener("click", () => updateCarousel(currentPage + 1));

    window.addEventListener("resize", () => {
      const pageCount = getPageCount();
      if (currentPage >= pageCount) {
        currentPage = Math.max(pageCount - 1, 0);
      }
      renderDots();
      updateCarousel(currentPage);
    });

    renderDots();
    updateCarousel(0);
  }

  function renderCarouselList(target, products) {
    if (!target) return;

    target.innerHTML = `
      <div class="hamster-carousel">
        <div class="hamster-carousel__viewport">
          <div class="hamster-carousel__track">
            ${products.map(featuredProductSlide).join("")}
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
          <p class="price">${SiteRouter.formatPrice(product.price)}</p>
          <ul class="check-list">${product.features.map((feature) => `<li>${feature}</li>`).join("")}</ul>
          <div class="button-row">
            <button class="button" type="button" id="detail-add-cart">カートに追加</button>
            ${quantityBadge(product.id)}
            <a class="button button--ghost" href="index.html?category=${product.category}">一覧に戻る</a>
          </div>
        </article>
      </div>
    `;

    document.getElementById("detail-add-cart")?.addEventListener("click", () => {
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
