(function () {
  let lastCartCount = null;
  const memberKey = "hamu_member";
  const notificationSeenKey = "hamu_seen_notifications";
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

  function loadSeenNotifications() {
    try {
      const parsed = JSON.parse(localStorage.getItem(notificationSeenKey) || "[]");
      return Array.isArray(parsed) ? new Set(parsed) : new Set();
    } catch (error) {
      return new Set();
    }
  }

  function saveSeenNotifications(seen) {
    localStorage.setItem(notificationSeenKey, JSON.stringify(Array.from(seen)));
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

  function createNotificationId(item) {
    return `${item.date || ""}|${item.title || ""}`;
  }

  async function setupNotifications() {
    const toggle = document.querySelector("[data-notification-toggle]");
    const panel = document.querySelector("[data-notification-panel]");
    const badge = document.querySelector("[data-notification-badge]");
    const list = document.querySelector("[data-notification-list]");
    if (!toggle || !panel || !badge || !list) return;

    let notifications = [];
    try {
      const response = await fetch(`${document.body.dataset.basePath || "./"}assets/data/news.json`);
      if (response.ok) {
        const data = await response.json();
        notifications = Array.isArray(data) ? data.slice(0, 8) : [];
      }
    } catch (error) {
      notifications = [];
    }

    const seen = loadSeenNotifications();
    const unreadCount = notifications.filter((item) => !seen.has(createNotificationId(item))).length;

    badge.hidden = unreadCount === 0;
    toggle.setAttribute("aria-label", unreadCount > 0 ? `通知 ${unreadCount}件` : "通知");

    if (!notifications.length) {
      list.innerHTML = '<li class="notification-empty">お知らせはありません。</li>';
    } else {
      const basePath = document.body.dataset.basePath || "./";
      list.innerHTML = notifications.map((item) => {
        const id = createNotificationId(item);
        const href = item.link ? `${basePath}${String(item.link).replace(/^\/+/, "")}` : `${basePath}pages/news.html`;
        const title = String(item.title || "お知らせ");
        const date = String(item.date || "");
        const unread = seen.has(id) ? "" : "data-unread=\"true\"";
        return `
          <li>
            <a class="notification-item-link" href="${href}" ${unread}>
              <span class="notification-item-title">${title}</span>
              <span class="notification-item-date">${date}</span>
            </a>
          </li>
        `;
      }).join("");
    }

    const closePanel = () => {
      panel.hidden = true;
      toggle.setAttribute("aria-expanded", "false");
    };

    const openPanel = () => {
      panel.hidden = false;
      toggle.setAttribute("aria-expanded", "true");
      const seenNow = loadSeenNotifications();
      notifications.forEach((item) => seenNow.add(createNotificationId(item)));
      saveSeenNotifications(seenNow);
      badge.hidden = true;
    };

    toggle.addEventListener("click", (event) => {
      event.preventDefault();
      if (panel.hidden) {
        openPanel();
      } else {
        closePanel();
      }
    });

    document.addEventListener("click", (event) => {
      if (panel.hidden) return;
      const target = event.target;
      if (!(target instanceof Node)) return;
      if (!panel.contains(target) && !toggle.contains(target)) {
        closePanel();
      }
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        closePanel();
      }
    });
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
    await setupNotifications();
    updateCartCount();
    updateMemberLink();
    document.addEventListener("cart:updated", updateCartCount);
    document.addEventListener("member:updated", updateMemberLink);
    await enforceMemberGuard();
  });
})();
