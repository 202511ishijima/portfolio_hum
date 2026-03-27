window.MembershipPage = (function () {
  const memberKey = "hamu_member";
  const backendBaseUrl = "http://localhost:8080";
  const threadRefreshMs = 10000;
  let threadTimer = null;

  function getDefaultMember() {
    return {
      name: "ゲスト",
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

  async function fetchInquiryThread(email) {
    if (!email) return [];
    try {
      const response = await fetch(`${backendBaseUrl}/api/inquiries/thread?email=${encodeURIComponent(email)}`);
      if (!response.ok) return [];
      const thread = await response.json();
      return Array.isArray(thread) ? thread : [];
    } catch (error) {
      return [];
    }
  }

  async function sendInquiryMessage(member, message, subject) {
    const payload = {
      name: String(member.name || "ゲスト").trim(),
      email: String(member.email || "").trim(),
      subject: String(subject || "マイページからのお問い合わせ").trim(),
      message: String(message || "").trim()
    };

    const response = await fetch(`${backendBaseUrl}/api/inquiries`, {
      method: "POST",
      headers: { "Content-Type": "application/json; charset=UTF-8" },
      body: JSON.stringify(payload)
    });

    const result = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(result.message || "送信に失敗しました。");
    }
    return result;
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
    return `<div class="media-placeholder" data-label="${item.imageLabel || "Reward image"}"></div>`;
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
    const statusLabel = member.loggedIn ? (member.status === "SUSPENDED" ? "停止中" : "利用中") : "ログイン前";
    const linkLabel = member.loggedIn ? "特典一覧を見る" : "ログイン";
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

  function escapeHtml(value) {
    return String(value || "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }

  function normalizeThread(messages) {
    return messages
      .filter((item) => item && item.body)
      .sort((a, b) => {
        const at = new Date(a.sentAt || 0).getTime();
        const bt = new Date(b.sentAt || 0).getTime();
        return at - bt;
      });
  }

  function renderThreadBubbles(messages) {
    return messages.map((item) => {
      const isAdmin = item.sender === "admin";
      const senderLabel = isAdmin ? "運営" : "あなた";
      const subjectHtml = !isAdmin && item.subject ? `<p class="chat-message__subject">${escapeHtml(item.subject)}</p>` : "";
      const bodyHtml = escapeHtml(item.body).replaceAll("\n", "<br>");
      return `
        <article class="chat-message ${isAdmin ? "chat-message--admin" : "chat-message--member"}">
          <div class="chat-message__meta">
            <span>${senderLabel}</span>
            <span>${formatReplyDate(item.sentAt)}</span>
          </div>
          <div class="chat-message__bubble">
            ${subjectHtml}
            <p>${bodyHtml}</p>
          </div>
        </article>
      `;
    }).join("");
  }

  function setComposeStatus(statusEl, message, isError) {
    if (!statusEl) return;
    statusEl.textContent = message;
    statusEl.className = "form-status";
    statusEl.dataset.state = isError ? "error" : "success";
  }

  async function renderMemberInquiryReplies() {
    const target = document.getElementById("member-inquiry-replies");
    if (!target) return;

    const member = getMember();
    if (!member.loggedIn || !member.email) {
      target.innerHTML = `
        <article class="panel">
          <h2>お問い合わせチャット</h2>
          <p>ログインすると、運営とのやり取り履歴を確認できます。</p>
        </article>
      `;
      return;
    }

    const thread = normalizeThread(await fetchInquiryThread(member.email));
    target.innerHTML = `
      <article class="panel chat-panel">
        <h2>お問い合わせチャット</h2>
        <p>送信した内容と運営からの返信を、時系列で確認できます。</p>
        <div class="chat-thread" id="inquiry-thread">
          ${thread.length ? renderThreadBubbles(thread) : '<p class="chat-empty">まだメッセージはありません。下の入力欄から送信できます。</p>'}
        </div>
        <form class="form-grid chat-compose" id="inquiry-compose-form">
          <label class="form-row">
            件名
            <select name="subject" required>
              <option value="">選択してください</option>
              <option>カフェ利用について</option>
              <option>お迎えについて</option>
              <option>飼育について</option>
              <option>その他</option>
            </select>
          </label>
          <label class="form-row">
            メッセージ
            <textarea name="message" rows="4" required placeholder="お問い合わせ内容を入力してください"></textarea>
          </label>
          <div class="button-row">
            <button class="button" type="submit">送信する</button>
          </div>
          <p class="form-status" id="inquiry-compose-status" aria-live="polite"></p>
        </form>
      </article>
    `;

    const threadEl = document.getElementById("inquiry-thread");
    if (threadEl) threadEl.scrollTop = threadEl.scrollHeight;

    const form = document.getElementById("inquiry-compose-form");
    const statusEl = document.getElementById("inquiry-compose-status");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const formData = new FormData(form);
      const subject = String(formData.get("subject") || "").trim();
      const message = String(formData.get("message") || "").trim();

      if (!subject) {
        setComposeStatus(statusEl, "件名を選択してください。", true);
        return;
      }

      if (!message) {
        setComposeStatus(statusEl, "メッセージを入力してください。", true);
        return;
      }

      try {
        await sendInquiryMessage(member, message, subject);
        form.reset();
        setComposeStatus(statusEl, "送信しました。", false);
        await refreshInquiryThread();
      } catch (error) {
        setComposeStatus(statusEl, error.message || "送信に失敗しました。", true);
      }
    });
  }

  async function refreshInquiryThread() {
    if (!document.getElementById("inquiry-thread")) return;

    const member = getMember();
    if (!member.loggedIn || !member.email) return;

    const threadEl = document.getElementById("inquiry-thread");
    const beforeBottom = threadEl.scrollHeight - threadEl.scrollTop - threadEl.clientHeight;
    const keepBottom = beforeBottom < 40;

    const thread = normalizeThread(await fetchInquiryThread(member.email));
    threadEl.innerHTML = thread.length
      ? renderThreadBubbles(thread)
      : '<p class="chat-empty">まだメッセージはありません。下の入力欄から送信できます。</p>';

    if (keepBottom) {
      threadEl.scrollTop = threadEl.scrollHeight;
    }
  }

  function startThreadPolling() {
    if (threadTimer) clearInterval(threadTimer);
    if (!document.getElementById("member-inquiry-replies")) return;

    threadTimer = setInterval(() => {
      refreshInquiryThread();
    }, threadRefreshMs);
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
      <a class="button" href="register.html">アカウント登録</a>
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
      showFormMessage(status, "必須項目を入力してください。", true);
      return;
    }

    try {
      const response = await fetch(`${backendBaseUrl}/api/members/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=UTF-8" },
        body: JSON.stringify(payload)
      });

      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(result.message || "アカウント作成に失敗しました。");
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
      showFormMessage(status, error.message || "登録に失敗しました。", true);
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
        points: Number(status?.points ?? current.points ?? 0),
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
    renderMemberInquiryReplies().then(startThreadPolling);
    renderMembershipActions();
    bindAuthForms();
    bindLogoutButton();
  });

  window.addEventListener("beforeunload", () => {
    if (threadTimer) {
      clearInterval(threadTimer);
      threadTimer = null;
    }
  });

  return {
    getMember,
    saveMember,
    logoutMember,
    renderMembershipActions
  };
})();
