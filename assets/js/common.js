(function () {
  let lastCartCount = null;
  const memberKey = "hamu_member";

  async function injectPartial(targetId, fileName) {
    const target = document.getElementById(targetId);
    if (!target) return;

    const basePath = document.body.dataset.basePath || "./";
    const response = await fetch(`${basePath}assets/partials/${fileName}`);
    const html = await response.text();
    target.innerHTML = html.replaceAll("{{base}}", basePath);
  }

  function updateCartCount() {
    const cart = JSON.parse(localStorage.getItem("hamu_cart") || "[]");
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);

    document.querySelectorAll("[data-cart-count]").forEach((node) => {
      node.textContent = String(count);
    });

    document.querySelectorAll(".icon-link--cart").forEach((link) => {
      link.setAttribute("aria-label", count > 0 ? `カート内商品 ${count}点` : "カート");
    });

    if (lastCartCount !== null && count > lastCartCount) {
      document.querySelectorAll(".icon-link--cart").forEach((link) => {
        link.classList.remove("is-bumped");
        void link.offsetWidth;
        link.classList.add("is-bumped");
      });
    }

    lastCartCount = count;
  }

  function getMember() {
    try {
      return JSON.parse(localStorage.getItem(memberKey) || "null");
    } catch (error) {
      return null;
    }
  }

  function updateMemberLink() {
    const member = getMember();
    const isLoggedIn = Boolean(member?.loggedIn && member?.name);
    const basePath = document.body.dataset.basePath || "./";

    document.querySelectorAll("[data-member-label]").forEach((node) => {
      node.textContent = isLoggedIn ? member.name : "会員";
    });

    document.querySelectorAll("[data-member-link]").forEach((node) => {
      node.setAttribute("href", isLoggedIn ? `${basePath}pages/mypage.html` : `${basePath}pages/login.html`);
      node.setAttribute("aria-label", isLoggedIn ? `${member.name} さんのマイページ` : "会員ページ");
    });
  }

  function setActiveNav() {
    const page = document.body.dataset.page;
    if (!page) return;
    document.querySelectorAll(`[data-nav="${page}"]`).forEach((link) => {
      link.classList.add("is-active");
    });
  }

  function setupMenu() {
    const toggle = document.querySelector(".menu-toggle");
    const nav = document.querySelector(".global-nav");
    if (!toggle || !nav) return;

    toggle.addEventListener("click", () => {
      const isOpen = nav.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", String(isOpen));
    });
  }

  function setupYear() {
    document.querySelectorAll("[data-year]").forEach((node) => {
      node.textContent = String(new Date().getFullYear());
    });
  }

  function setupFavicon() {
    const basePath = document.body.dataset.basePath || "./";
    const href = `${basePath}image/アイコン.png`;
    let link = document.querySelector('link[rel="icon"]');
    if (!link) {
      link = document.createElement("link");
      link.rel = "icon";
      document.head.appendChild(link);
    }
    link.href = href;
  }

  document.addEventListener("DOMContentLoaded", async () => {
    await injectPartial("site-header", "header.html");
    await injectPartial("site-footer", "footer.html");
    document.body.classList.add("has-fixed-header");
    setActiveNav();
    setupMenu();
    setupYear();
    setupFavicon();
    updateCartCount();
    updateMemberLink();
    document.addEventListener("cart:updated", updateCartCount);
    document.addEventListener("member:updated", updateMemberLink);
  });
})();
