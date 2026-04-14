(function () {
  let lastCartCount = null;
  const memberKey = "hamu_member";
  const backendBaseUrl = (() => {
    const { hostname, port, origin } = window.location;
    if (hostname.endsWith("github.io")) return "https://portfolio-hum.onrender.com";
    if (hostname.endsWith("onrender.com")) return origin;
    if ((hostname === "127.0.0.1" || hostname === "localhost") && port === "3000") return "http://localhost:8080";
    return origin;
  })();

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

  function saveMember(member) {
    localStorage.setItem(memberKey, JSON.stringify(member));
    document.dispatchEvent(new Event("member:updated"));
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

  function setupPageTopButton() {
    const button = document.querySelector("[data-page-top]");
    if (!button) return;

    function updateVisibility() {
      const threshold = window.innerHeight / 2;
      button.classList.toggle("is-visible", window.scrollY > threshold);
    }

    window.addEventListener("scroll", updateVisibility, { passive: true });
    button.addEventListener("click", () => {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      });
    });

    updateVisibility();
  }

  async function fetchMemberStatus(email) {
    if (!email) return null;

    try {
      const response = await fetch(`${backendBaseUrl}/api/members/status?email=${encodeURIComponent(email)}`);
      if (!response.ok) return null;
      return await response.json();
    } catch (error) {
      return null;
    }
  }

  async function enforceMemberGuard() {
    if (document.body.dataset.memberGuard !== "active") return;

    const member = getMember();
    if (!member?.loggedIn || !member?.email) return;

    const status = await fetchMemberStatus(member.email);
    if (!status) return;

    saveMember({
      ...member,
      name: status.name || member.name,
      email: status.email || member.email,
      points: Number(status.points ?? member.points ?? 0),
      status: status.status || member.status
    });

    if (status.status === "SUSPENDED") {
      const basePath = document.body.dataset.basePath || "./";
      const suspendedPage = `${basePath}pages/account-suspended.html`;
      if (!window.location.href.includes("/pages/account-suspended.html")) {
        window.location.href = suspendedPage;
      }
    }
  }

  document.addEventListener("DOMContentLoaded", async () => {
    await injectPartial("site-header", "header.html");
    await injectPartial("site-footer", "footer.html");
    document.body.classList.add("has-fixed-header");
    setActiveNav();
    setupMenu();
    setupYear();
    setupFavicon();
    setupPageTopButton();
    updateCartCount();
    updateMemberLink();
    document.addEventListener("cart:updated", updateCartCount);
    document.addEventListener("member:updated", updateMemberLink);
    await enforceMemberGuard();
  });
})();
