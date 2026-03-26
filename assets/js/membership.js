window.MembershipPage = (function () {
  const memberKey = "hamu_member";
  const backendBaseUrl = "http://localhost:8080";

  function getDefaultMember() {
    return {
      name: "ゲストさま",
      points: 0,
      email: "",
      loggedIn: false,
      status: "ACTIVE"
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

  async function fetchInquiryReplies(email) {
    if (!email) return [];
    try {
      const response = await fetch(`${backendBaseUrl}/api/inquiries/replies?email=${encodeURIComponent(email)}`);
      if (!response.ok) return [];
      const replies = await response.json();
      return Array.isArray(replies) ? replies : [];
    } catch (error) {
      return [];
    }
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
    const statusLabel = member.loggedIn
      ? (member.status === "SUSPENDED" ? "停止中" : "利用中")
      : "ログイン前";
    const linkLabel = member.loggedIn ? "景品一覧を見る" : "ログインする";
    const linkHref = member.loggedIn ? "rewards.html" : "login.html";

    target.innerHTML = `
      <div class="section-stack">
        <div class="stats-row">
          <div class="stats-card">会員名<strong>${name}</strong></div>
          <div class="stats-card">現在ポイント<strong>${points}</strong></div>
          <div class="stats-card">アカウント状態<strong>${statusLabel}</strong></div>
          <div class="stats-card">次のステップ<strong><a href="${linkHref}">${linkLabel}</a></strong></div>
        </div>
        ${member.loggedIn ? '<div class="button-row"><button class="button button--ghost" type="button" id="logout-button">ログアウト</button></div>' : ""}
      </div>
    `;
  }

  function formatReplyDate(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("ja-JP", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });
  }

  async function renderMemberInquiryReplies() {
    const target = document.getElementById("member-inquiry-replies");
    if (!target) return;

    const member = getMember();
    if (!member.loggedIn || !member.email) {
      target.innerHTML = `
        <article class="panel">
          <h2>お問い合わせへの返信</h2>
          <p>ログインすると、管理者からの返信を確認できます。</p>
        </article>
      `;
      return;
    }

    const replies = await fetchInquiryReplies(member.email);
    if (replies.length === 0) {
      target.innerHTML = `
        <article class="panel">
          <h2>お問い合わせへの返信</h2>
          <p>まだ返信は届いていません。返信があるとここに表示されます。</p>
        </article>
      `;
      return;
    }

    target.innerHTML = `
      <article class="panel">
        <h2>お問い合わせへの返信</h2>
        <div class="news-list">
          ${replies.map((item) => `
            <article class="news-card">
              <p class="eyebrow">お問い合わせ #${item.inquiryId}</p>
              <h3>${formatReplyDate(item.sentAt)}</h3>
              <p>${item.reply}</p>
            </article>
          `).join("")}
        </div>
      </article>
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

  function showFormMessage(target, message, isError) {
    if (!target) {
      alert(message);
      return;
    }

    target.textContent = message;
    target.className = isError ? "form-message error" : "form-message success";
  }

  async function submitRegisterForm(form) {
    const status = document.getElementById("register-status");
    const formData = new FormData(form);
    const payload = {
      name: String(formData.get("name") || "").trim(),
      email: String(formData.get("email") || "").trim(),
      password: String(formData.get("password") || "")
    };

    if (!payload.name || !payload.email || !payload.password) {
      showFormMessage(status, "未入力の項目があります。すべて入力してください。", true);
      return;
    }

    try {
      const response = await fetch(`${backendBaseUrl}/api/members/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=UTF-8"
        },
        body: JSON.stringify(payload)
      });

      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(result.message || "アカウントを作成できませんでした。");
      }

      saveMember({
        name: result.name || payload.name,
        email: result.email || payload.email,
        points: Number(result.points || 0),
        loggedIn: true,
        status: result.status || "ACTIVE"
      });

      showFormMessage(status, result.message || "アカウントを作成しました。", false);
      setTimeout(() => {
        window.location.href = "mypage.html";
      }, 600);
    } catch (error) {
      showFormMessage(
        status,
        error.message || "登録できませんでした。Spring Boot が起動しているか確認してください。",
        true
      );
    }
  }

  function getSuspendedPagePath() {
    const basePath = document.body.dataset.basePath || "../";
    return `${basePath}pages/account-suspended.html`;
  }

  function bindAuthForms() {
    const register = document.getElementById("register-form");
    const login = document.getElementById("login-form");

    register?.addEventListener("submit", async (event) => {
      event.preventDefault();
      await submitRegisterForm(register);
    });

    login?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const formData = new FormData(login);
      const email = String(formData.get("email") || "").trim();
      const current = getMember();
      const status = await fetchMemberStatus(email);

      if (status?.status === "SUSPENDED") {
        saveMember({
          ...current,
          name: status.name || email.split("@")[0],
          email,
          points: Number(status.points || 0),
          loggedIn: true,
          status: "SUSPENDED"
        });
        window.location.href = getSuspendedPagePath();
        return;
      }

      saveMember({
        ...current,
        name: status?.name || (current.loggedIn && current.email === email ? current.name : email.split("@")[0]),
        email,
        points: Number(status?.points ?? current.points ?? 128),
        loggedIn: true,
        status: status?.status || "ACTIVE"
      });
      window.location.href = "mypage.html";
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderNews();
    renderRewards();
    renderCafeMenus();
    renderMemberSummary();
    renderMemberInquiryReplies();
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
