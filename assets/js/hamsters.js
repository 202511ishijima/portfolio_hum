window.HamstersPage = (function () {
  let hamstersCache = [];

  async function getHamsters() {
    if (hamstersCache.length) return hamstersCache;
    hamstersCache = await SiteRouter.fetchJSON("hamsters.json");
    return hamstersCache;
  }

  function imageHtml(filename, alt, detail = false) {
    if (!filename) {
      return detail
        ? '<div class="detail-card detail-card__media"></div>'
        : '<div class="hamster-card__media"></div>';
    }

    const className = detail ? "detail-card media-photo" : "media-photo";
    return `<figure class="${className}"><img src="${SiteRouter.getBasePath()}image/${filename}" alt="${alt}"></figure>`;
  }

  function hamsterCard(hamster) {
    return `
      <article class="hamster-card">
        ${imageHtml(hamster.image, hamster.species)}
        <div class="hamster-card__meta">
          <span class="chip">${hamster.species}</span>
          <span class="status-chip">飼いやすさ ${hamster.ease}</span>
        </div>
        <h3>${hamster.species}</h3>
        <p class="text-soft">性格: ${hamster.personality}</p>
        <p>${hamster.description}</p>
        <p class="price">${SiteRouter.formatPrice(hamster.price)}</p>
        <a class="button button--ghost" href="${SiteRouter.getBasePath()}hamsters/detail.html?id=${hamster.id}">詳細を見る</a>
      </article>
    `;
  }

  function featuredSlide(hamster) {
    return `
      <article class="hamster-carousel__slide">
        ${imageHtml(hamster.image, hamster.species)}
        <div class="hamster-card__meta">
          <span class="chip">${hamster.species}</span>
          <span class="status-chip">飼いやすさ ${hamster.ease}</span>
        </div>
        <h3>${hamster.species}</h3>
        <p class="text-soft">性格: ${hamster.personality}</p>
        <p>${hamster.description}</p>
        <p class="price">${SiteRouter.formatPrice(hamster.price)}</p>
        <div class="button-row">
          <a class="button" href="${SiteRouter.getBasePath()}hamsters/detail.html?id=${hamster.id}">詳細を見る</a>
        </div>
      </article>
    `;
  }

  function setupCarousel(target) {
    const track = target.querySelector(".hamster-carousel__track");
    const slides = Array.from(target.querySelectorAll(".hamster-carousel__slide"));
    const prev = target.querySelector("[data-carousel-prev]");
    const next = target.querySelector("[data-carousel-next]");
    const dotsWrap = target.querySelector(".hamster-carousel__dots");
    if (!track || !slides.length || !dotsWrap) return;

    let currentPage = 0;

    function getSlidesPerView() {
      return window.innerWidth >= 900 ? 3 : 1;
    }

    function getPageCount() {
      return Math.ceil(slides.length / getSlidesPerView());
    }

    function renderDots() {
      const pageCount = getPageCount();
      dotsWrap.innerHTML = Array.from({ length: pageCount }, (_, index) => {
        return `<button type="button" class="hamster-carousel__dot" data-carousel-dot="${index}" aria-label="${index + 1}ページ目へ移動"></button>`;
      }).join("");

      Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot) => {
        dot.addEventListener("click", () => {
          updateCarousel(Number(dot.dataset.carouselDot));
        });
      });
    }

    function updateCarousel(page) {
      const pageCount = getPageCount();
      if (!pageCount) return;

      currentPage = (page + pageCount) % pageCount;
      track.style.transform = `translateX(-${currentPage * 100}%)`;

      Array.from(dotsWrap.querySelectorAll("[data-carousel-dot]")).forEach((dot, index) => {
        dot.classList.toggle("is-active", index === currentPage);
      });
    }

    prev?.addEventListener("click", () => updateCarousel(currentPage - 1));
    next?.addEventListener("click", () => updateCarousel(currentPage + 1));

    window.addEventListener("resize", () => {
      const pageCount = getPageCount();
      if (currentPage >= pageCount) {
        currentPage = Math.max(pageCount - 1, 0);
      }
      renderDots();
      updateCarousel(currentPage);
    });

    renderDots();
    updateCarousel(0);
  }

  async function renderFeatured() {
    const target = document.getElementById("featured-hamsters");
    if (!target) return;
    const hamsters = await getHamsters();

    target.innerHTML = `
      <div class="hamster-carousel">
        <div class="hamster-carousel__viewport">
          <div class="hamster-carousel__track">
            ${hamsters.map(featuredSlide).join("")}
          </div>
        </div>
        <div class="hamster-carousel__controls">
          <button class="button button--ghost" type="button" data-carousel-prev>前へ</button>
          <div class="hamster-carousel__dots"></div>
          <button class="button button--ghost" type="button" data-carousel-next>次へ</button>
        </div>
      </div>
    `;

    setupCarousel(target);
  }

  async function renderIndex() {
    const target = document.getElementById("hamster-list");
    if (!target) return;
    const hamsters = await getHamsters();
    target.innerHTML = hamsters.map(hamsterCard).join("");
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
        ${imageHtml(hamster.image, hamster.species, true)}
        <article class="detail-card">
          <p class="eyebrow">${hamster.species}</p>
          <h1>${hamster.species}</h1>
          <p class="text-soft">${hamster.description}</p>
          <div class="chip-list">
            <span class="chip">性格: ${hamster.personality}</span>
            <span class="chip">飼いやすさ: ${hamster.ease}</span>
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
