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
      link.setAttribute("aria-label", count > 0 ? `カート 件数 ${count}点` : "カート");
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
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
    updateVisibility();
  }

  function loadSeenNotifications() {
    try {
      const parsed = JSON.parse(localStorage.getItem(notificationSeenKey) || "[]");
      return Array.isArray(parsed) ? new Set(parsed) : new Set();
    } catch (error) {
      return new Set();
    }
  }

  function saveSeenNotifications(seenSet) {
    localStorage.setItem(notificationSeenKey, JSON.stringify(Array.from(seenSet)));
  }

  function normalizePath(urlLike) {
    try {
      const url = new URL(urlLike, window.location.origin);
      return url.pathname.replace(/\/+$/, "");
    } catch (error) {
      return "";
    }
  }

  function createNotificationId(item) {
    if (item.type === "reply") {
      return `reply|${item.replyId || item.inquiryId || ""}|${item.sentAt || ""}`;
    }
    return `news|${item.date || ""}|${item.title || ""}`;
  }

  function parseTime(value) {
    const time = new Date(value || "").getTime();
    return Number.isNaN(time) ? 0 : time;
  }

  async function fetchInquiryThread(email) {
    if (!email) return [];
    try {
      const response = await fetch(`${backendBaseUrl}/api/inquiries/thread?email=${encodeURIComponent(email)}`);
      if (!response.ok) return [];
      const data = await response.json();
      return Array.isArray(data) ? data : [];
    } catch (error) {
      return [];
    }
  }

  function buildNotificationItems(newsItems, replyItems, basePath) {
    const fromNews = newsItems.map((item) => ({
      type: "news",
      title: String(item.title || "お知らせ"),
      date: String(item.date || ""),
      sentAt: item.date,
      href: item.link ? `${basePath}${String(item.link).replace(/^\/+/, "")}` : `${basePath}pages/news.html`
    }));

    const fromReplies = replyItems
      .filter((item) => item && item.sender === "admin" && item.body)
      .map((item) => ({
        type: "reply",
        title: "お問い合わせに返信が届いています",
        date: String(item.sentAt || ""),
        sentAt: item.sentAt,
        inquiryId: item.inquiryId,
        replyId: item.replyId,
        href: `${basePath}pages/mypage.html#member-inquiry-replies`
      }));

    return [...fromReplies, ...fromNews].sort((a, b) => parseTime(b.sentAt) - parseTime(a.sentAt));
  }

  function markCurrentPageAsSeen(items, seenSet) {
    const currentPath = normalizePath(window.location.href);
    let changed = false;
    items.forEach((item) => {
      const itemPath = normalizePath(item.href);
      if (itemPath && itemPath === currentPath) {
        const id = createNotificationId(item);
        if (!seenSet.has(id)) {
          seenSet.add(id);
          changed = true;
        }
      }
    });
    if (changed) saveSeenNotifications(seenSet);
  }

  function renderNotificationList(listEl, items, seenSet) {
    if (!items.length) {
      listEl.innerHTML = '<li class="notification-empty">お知らせはありません。</li>';
      return;
    }

    listEl.innerHTML = items.map((item) => {
      const id = createNotificationId(item);
      const unread = seenSet.has(id) ? "" : 'data-unread="true"';
      return `
        <li>
          <a class="notification-item-link" href="${item.href}" data-notification-id="${id}" ${unread}>
            <span class="notification-item-title">${item.title}</span>
            <span class="notification-item-date">${item.date}</span>
          </a>
        </li>
      `;
    }).join("");
  }

  async function setupNotifications() {
    const toggle = document.querySelector("[data-notification-toggle]");
    const panel = document.querySelector("[data-notification-panel]");
    const badge = document.querySelector("[data-notification-badge]");
    const list = document.querySelector("[data-notification-list]");
    if (!toggle || !panel || !badge || !list) return;

    const basePath = document.body.dataset.basePath || "./";
    const member = getMember();

    let news = [];
    try {
      const response = await fetch(`${basePath}assets/data/news.json`);
      if (response.ok) {
        const data = await response.json();
        news = Array.isArray(data) ? data.slice(0, 8) : [];
      }
    } catch (error) {
      news = [];
    }

    const replies = (member?.loggedIn && member?.email) ? await fetchInquiryThread(member.email) : [];
    const items = buildNotificationItems(news, replies, basePath);
    const seen = loadSeenNotifications();
    markCurrentPageAsSeen(items, seen);
    renderNotificationList(list, items, seen);

    const unreadCount = items.filter((item) => !seen.has(createNotificationId(item))).length;
    badge.hidden = unreadCount === 0;
    toggle.setAttribute("aria-label", unreadCount > 0 ? `通知 ${unreadCount}件` : "通知");

    const closePanel = () => {
      panel.hidden = true;
      toggle.setAttribute("aria-expanded", "false");
    };

    const openPanel = () => {
      panel.hidden = false;
      toggle.setAttribute("aria-expanded", "true");
    };

    toggle.addEventListener("click", (event) => {
      event.preventDefault();
      if (panel.hidden) openPanel();
      else closePanel();
    });

    list.addEventListener("click", (event) => {
      const target = event.target;
      if (!(target instanceof Element)) return;
      const link = target.closest(".notification-item-link");
      if (!link) return;
      const id = link.getAttribute("data-notification-id");
      if (!id) return;

      const seenNow = loadSeenNotifications();
      seenNow.add(id);
      saveSeenNotifications(seenNow);
      link.removeAttribute("data-unread");

      const remain = Array.from(list.querySelectorAll(".notification-item-link[data-unread='true']")).length;
      badge.hidden = remain === 0;
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
      if (event.key === "Escape") closePanel();
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
