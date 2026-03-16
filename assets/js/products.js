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

  function cardTemplate(product) {
    return `
      <article class="product-card">
        ${productMedia(product)}
        <div class="product-card__meta">
          <span class="chip">${categoryLabel(product.category)}</span>
          ${product.badge ? `<span class="status-chip">${product.badge}</span>` : ""}
        </div>
        <h3>${product.name}</h3>
        <p class="text-soft">${product.shortDescription}</p>
        <p class="price">${SiteRouter.formatPrice(product.price)}</p>
        <div class="button-row">
          <a class="button button--ghost" href="${SiteRouter.getBasePath()}products/detail.html?id=${product.id}">詳細を見る</a>
          <button class="button" type="button" data-add-product="${product.id}">カートに追加</button>
        </div>
      </article>
    `;
  }

  function bindAddButtons(products) {
    document.querySelectorAll("[data-add-product]").forEach((button) => {
      button.addEventListener("click", () => {
        const product = products.find((item) => item.id === button.dataset.addProduct);
        if (!product) return;
        Cart.addItem({ id: product.id, name: product.name, price: product.price, categoryLabel: categoryLabel(product.category) });
      });
    });
  }

  function renderList(target, products) {
    if (target) target.innerHTML = products.map(cardTemplate).join("");
  }

  async function renderHomeSections() {
    const products = await getProducts();
    renderList(document.getElementById("starter-products"), products.filter((item) => item.category === "starter"));
    renderList(document.getElementById("popular-products"), products.filter((item) => item.popular).slice(0, 6));
    bindAddButtons(products);
  }

  async function renderProductLanding() {
    const target = document.getElementById("product-category-preview");
    if (!target) return;
    const products = await getProducts();
    target.innerHTML = ["cage", "food", "toy", "care", "starter"].map((category) => {
      const first = products.find((item) => item.category === category);
      return `<article class="info-card"><h3>${categoryLabel(category)}</h3><p>${first ? first.shortDescription : ""}</p><a class="button button--ghost" href="index.html?category=${category}">一覧を見る</a></article>`;
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
            <a class="button button--ghost" href="index.html?category=${product.category}">同じカテゴリを見る</a>
          </div>
        </article>
      </div>
    `;

    document.getElementById("detail-add-cart")?.addEventListener("click", () => {
      Cart.addItem({ id: product.id, name: product.name, price: product.price, categoryLabel: categoryLabel(product.category) });
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderHomeSections();
    renderProductLanding();
    renderProductListPage();
    renderProductDetailPage();
  });
})();
