window.MembershipPage = (function () {
  const memberKey = "hamu_member";

  function getDefaultMember() {
    return {
      name: "ゲストさま",
      points: 0,
      email: "",
      loggedIn: false
    };
  }

  function getMember() {
    try {
      const stored = JSON.parse(localStorage.getItem(memberKey) || "null");
      if (!stored) return getDefaultMember();
      return { ...getDefaultMember(), ...stored };
    } catch (error) {
      return getDefaultMember();
    }
  }

  function saveMember(member) {
    localStorage.setItem(memberKey, JSON.stringify(member));
    document.dispatchEvent(new Event("member:updated"));
  }

  function logoutMember() {
    saveMember(getDefaultMember());
  }

  async function renderNews() {
    const target = document.getElementById("home-news");
    const listPage = document.getElementById("news-list");
    if (!target && !listPage) return;

    const news = await SiteRouter.fetchJSON("news.json");

    if (target) {
      target.innerHTML = news.slice(0, 3).map((item) => `
        <article class="news-card">
          <p class="eyebrow">${item.date}</p>
          <h3>${item.title}</h3>
          <p>${item.body}</p>
        </article>
      `).join("");
    }

    if (listPage) {
      listPage.innerHTML = news.map((item) => `
        <article class="news-card">
          <p class="eyebrow">${item.date}</p>
          <h2>${item.title}</h2>
          <p>${item.body}</p>
        </article>
      `).join("");
    }
  }

  function rewardMedia(item, basePath) {
    if (item.image) {
      return `<figure class="media-photo"><img src="${basePath}image/${item.image}" alt="${item.name}"></figure>`;
    }
    return `<div class="media-placeholder" data-label="${item.imageLabel || "景品画像を追加"}"></div>`;
  }

  async function renderRewards() {
    const target = document.getElementById("rewards-list");
    const preview = document.getElementById("membership-rewards");
    if (!target && !preview) return;

    const rewards = await SiteRouter.fetchJSON("rewards.json");
    const basePath = SiteRouter.getBasePath();
    const html = rewards.map((item) => `
      <article class="reward-card">
        ${rewardMedia(item, basePath)}
        <h3>${item.name}</h3>
        <p>${item.description}</p>
        <p class="price">${item.points} pt</p>
      </article>
    `).join("");

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
      if (drinkTarget) drinkTarget.innerHTML = menu.filter((item) => item.type === "drink").map(cardHtml).join("");
      if (foodTarget) foodTarget.innerHTML = menu.filter((item) => item.type === "food").map(cardHtml).join("");
    }

    if (treatTarget) {
      const treats = await SiteRouter.fetchJSON("treats.json");
      treatTarget.innerHTML = treats.map(cardHtml).join("");
    }
  }

  function renderMemberSummary() {
    const target = document.getElementById("member-summary");
    if (!target) return;

    const member = getMember();
    const name = member.loggedIn ? member.name : "未ログイン";
    const points = member.loggedIn ? `${member.points} pt` : "ログインすると表示されます";
    const linkLabel = member.loggedIn ? "景品一覧を見る" : "ログインする";
    const linkHref = member.loggedIn ? "rewards.html" : "login.html";

    target.innerHTML = `
      <div class="section-stack">
        <div class="stats-row">
          <div class="stats-card">会員名<strong>${name}</strong></div>
          <div class="stats-card">現在ポイント<strong>${points}</strong></div>
          <div class="stats-card">次のステップ<strong><a href="${linkHref}">${linkLabel}</a></strong></div>
        </div>
        ${member.loggedIn ? '<div class="button-row"><button class="button button--ghost" type="button" id="logout-button">ログアウト</button></div>' : ""}
      </div>
    `;
  }

  function renderMembershipActions() {
    const target = document.getElementById("membership-actions");
    if (!target) return;

    const member = getMember();
    if (member.loggedIn) {
      target.innerHTML = "";
      return;
    }

    target.innerHTML = `
      <a class="button" href="register.html">アカウント作成</a>
      <a class="button button--ghost" href="login.html">ログイン</a>
    `;
  }

  function bindLogoutButton() {
    document.getElementById("logout-button")?.addEventListener("click", () => {
      logoutMember();
      renderMemberSummary();
      window.location.href = "login.html";
    });
  }

  function bindAuthForms() {
    const register = document.getElementById("register-form");
    const login = document.getElementById("login-form");

    register?.addEventListener("submit", (event) => {
      event.preventDefault();
      const formData = new FormData(register);
      saveMember({
        name: String(formData.get("name") || "新規会員さま"),
        email: String(formData.get("email") || ""),
        points: 100,
        loggedIn: true
      });
      window.location.href = "mypage.html";
    });

    login?.addEventListener("submit", (event) => {
      event.preventDefault();
      const formData = new FormData(login);
      const email = String(formData.get("email") || "member@example.com");
      const current = getMember();
      saveMember({
        ...current,
        name: current.loggedIn && current.email === email
          ? current.name
          : email.split("@")[0],
        email,
        points: current.points || 128,
        loggedIn: true
      });
      window.location.href = "mypage.html";
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderNews();
    renderRewards();
    renderCafeMenus();
    renderMemberSummary();
    renderMembershipActions();
    bindAuthForms();
    bindLogoutButton();
  });

  return {
    getMember,
    saveMember,
    logoutMember,
    renderMembershipActions
  };
})();
