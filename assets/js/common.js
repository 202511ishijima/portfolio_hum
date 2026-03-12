(function () {
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

  document.addEventListener("DOMContentLoaded", async () => {
    await injectPartial("site-header", "header.html");
    await injectPartial("site-footer", "footer.html");
    setActiveNav();
    setupMenu();
    setupYear();
    updateCartCount();
    document.addEventListener("cart:updated", updateCartCount);
  });
})();
