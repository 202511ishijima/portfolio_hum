window.MembershipPage = (function () {
  const memberKey = "hamu_member";

  function getMember() {
    return JSON.parse(localStorage.getItem(memberKey) || '{"name":"ゲストさま","points":128}');
  }

  function saveMember(member) {
    localStorage.setItem(memberKey, JSON.stringify(member));
  }

  async function renderNews() {
    const target = document.getElementById("home-news");
    const listPage = document.getElementById("news-list");
    if (!target && !listPage) return;
    const news = await SiteRouter.fetchJSON("news.json");

    if (target) {
      target.innerHTML = news.slice(0, 3).map((item) => `<article class="news-card"><p class="eyebrow">${item.date}</p><h3>${item.title}</h3><p>${item.body}</p></article>`).join("");
    }

    if (listPage) {
      listPage.innerHTML = news.map((item) => `<article class="news-card"><p class="eyebrow">${item.date}</p><h2>${item.title}</h2><p>${item.body}</p></article>`).join("");
    }
  }

  async function renderRewards() {
    const target = document.getElementById("rewards-list");
    const preview = document.getElementById("membership-rewards");
    if (!target && !preview) return;
    const rewards = await SiteRouter.fetchJSON("rewards.json");
    const html = rewards.map((item) => `<article class="reward-card"><h3>${item.name}</h3><p>${item.description}</p><p class="price">${item.points} pt</p></article>`).join("");
    if (target) target.innerHTML = html;
    if (preview) preview.innerHTML = html;
  }

  async function renderCafeMenus() {
    const drinkTarget = document.getElementById("cafe-drink-list");
    const foodTarget = document.getElementById("cafe-food-list");
    const treatTarget = document.getElementById("cafe-treats-list");
    if (!drinkTarget && !foodTarget && !treatTarget) return;
    const basePath = SiteRouter.getBasePath();
    const cardHtml = (item) => {
      const media = item.image
        ? `<figure class="media-photo"><img src="${basePath}image/${item.image}" alt="${item.name}"></figure>`
        : `<div class="menu-card__media"></div>`;
      return `<article class="menu-card">${media}<h3>${item.name}</h3><p>${item.description}</p><p class="price">${SiteRouter.formatPrice(item.price)}</p></article>`;
    };

    if (drinkTarget || foodTarget) {
      const menu = await SiteRouter.fetchJSON("cafe_menu.json");
      if (drinkTarget) {
        drinkTarget.innerHTML = menu.filter((item) => item.type === "drink").map(cardHtml).join("");
      }
      if (foodTarget) {
        foodTarget.innerHTML = menu.filter((item) => item.type === "food").map(cardHtml).join("");
      }
    }

    if (treatTarget) {
      const treats = await SiteRouter.fetchJSON("treats.json");
      treatTarget.innerHTML = treats.map((item) => {
        const media = item.image
          ? `<figure class="media-photo"><img src="${basePath}image/${item.image}" alt="${item.name}"></figure>`
          : `<div class="menu-card__media"></div>`;
        return `<article class="menu-card">${media}<h3>${item.name}</h3><p>${item.description}</p><p class="price">${SiteRouter.formatPrice(item.price)}</p></article>`;
      }).join("");
    }
  }

  function renderMemberSummary() {
    const target = document.getElementById("member-summary");
    if (!target) return;
    const member = getMember();
    target.innerHTML = `
      <div class="stats-row">
        <div class="stats-card">会員名<strong>${member.name}</strong></div>
        <div class="stats-card">現在ポイント<strong>${member.points} pt</strong></div>
        <div class="stats-card">景品交換先<strong><a href="rewards.html">限定グッズ一覧</a></strong></div>
      </div>
    `;
  }

  function bindAuthForms() {
    const register = document.getElementById("register-form");
    const login = document.getElementById("login-form");

    register?.addEventListener("submit", (event) => {
      event.preventDefault();
      const formData = new FormData(register);
      saveMember({ name: String(formData.get("name") || "新規会員さま"), points: 100 });
      window.location.href = "mypage.html";
    });

    login?.addEventListener("submit", (event) => {
      event.preventDefault();
      const formData = new FormData(login);
      saveMember({ name: String(formData.get("email") || "member@example.com").split("@")[0], points: 128 });
      window.location.href = "mypage.html";
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderNews();
    renderRewards();
    renderCafeMenus();
    renderMemberSummary();
    bindAuthForms();
  });
})();
