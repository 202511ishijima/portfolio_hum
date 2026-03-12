window.HamstersPage = (function () {
  let hamstersCache = [];

  async function getHamsters() {
    if (hamstersCache.length) return hamstersCache;
    hamstersCache = await SiteRouter.fetchJSON("hamsters.json");
    return hamstersCache;
  }

  function hamsterCard(hamster) {
    return `
      <article class="hamster-card">
        <div class="hamster-card__media"></div>
        <div class="hamster-card__meta">
          <span class="chip">${hamster.species}</span>
          <span class="status-chip">飼いやすさ ${hamster.ease}</span>
        </div>
        <h3>${hamster.name}</h3>
        <p class="text-soft">${hamster.personality}</p>
        <p class="price">${SiteRouter.formatPrice(hamster.price)}</p>
        <a class="button button--ghost" href="${SiteRouter.getBasePath()}hamsters/detail.html?id=${hamster.id}">詳細を見る</a>
      </article>
    `;
  }

  async function renderFeatured() {
    const target = document.getElementById("featured-hamsters");
    if (!target) return;
    const hamsters = await getHamsters();
    target.innerHTML = hamsters.slice(0, 3).map(hamsterCard).join("");
  }

  async function renderIndex() {
    const list = document.getElementById("hamster-list");
    if (!list) return;
    const hamsters = await getHamsters();
    list.innerHTML = hamsters.map(hamsterCard).join("");
  }

  async function renderDetail() {
    const target = document.getElementById("hamster-detail");
    if (!target) return;
    const hamsters = await getHamsters();
    const id = SiteRouter.getQueryParam("id") || hamsters[0]?.id;
    const hamster = hamsters.find((item) => item.id === id);
    if (!hamster) {
      target.innerHTML = '<div class="empty-state">生体情報が見つかりませんでした。</div>';
      return;
    }

    target.innerHTML = `
      <div class="detail-layout">
        <div class="detail-card detail-card__media"></div>
        <article class="detail-card">
          <p class="eyebrow">${hamster.species}</p>
          <h1>${hamster.name}</h1>
          <p class="text-soft">${hamster.description}</p>
          <div class="chip-list">
            <span class="chip">性格: ${hamster.personality}</span>
            <span class="chip">飼いやすさ: ${hamster.ease}</span>
            <span class="chip">性別: ${hamster.sex}</span>
          </div>
          <p class="price">${SiteRouter.formatPrice(hamster.price)}</p>
          <ul class="check-list">
            <li>${hamster.feature1}</li>
            <li>${hamster.feature2}</li>
            <li>${hamster.feature3}</li>
          </ul>
          <div class="button-row">
            <a class="button" href="../pages/contact.html">問い合わせる</a>
            <a class="button button--ghost" href="index.html">一覧に戻る</a>
          </div>
        </article>
      </div>
    `;
  }

  document.addEventListener("DOMContentLoaded", () => {
    renderFeatured();
    renderIndex();
    renderDetail();
  });
})();
