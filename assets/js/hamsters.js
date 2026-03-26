const HAMSTER_SUMMARY_API = "http://localhost:8080/api/hamsters/summary";

async function getHamsterMetadata() {
  const basePath =
    window.SiteRouter && typeof window.SiteRouter.getBasePath === "function"
      ? window.SiteRouter.getBasePath()
      : "";
  const response = await fetch(`${basePath}assets/data/hamsters.json`);
  if (!response.ok) throw new Error("Failed to fetch hamsters");
  const hamsters = await response.json();
  return {
    basePath,
    items: hamsters.map((hamster) => ({
      ...hamster,
      image: hamster.image.replace("../", basePath),
    })),
  };
}

async function getHamsterSummaries() {
  const response = await fetch(HAMSTER_SUMMARY_API);
  if (!response.ok) {
    throw new Error("Failed to fetch hamster summary");
  }
  return response.json();
}

function mergeHamsterData(metadataItems, summaries) {
  const metadataMap = new Map(
    metadataItems.map((hamster) => [hamster.species, hamster]),
  );

  return summaries.map((summary) => {
    const meta = metadataMap.get(summary.species) || {};
    return {
      species: summary.species,
      count: summary.count,
      maleCount: summary.maleCount,
      femaleCount: summary.femaleCount,
      ageRange: summary.ageRange,
      personalityTrend:
        meta.personalityTrend ||
        "それぞれの個性を見比べながら、やさしくご案内しています。",
      comment:
        meta.comment ||
        "気になる子がいましたら、店頭またはお問い合わせにてご案内いたします。",
      image: meta.image || "",
    };
  });
}

async function getHamsters() {
  const metadata = await getHamsterMetadata();

  try {
    const summaries = await getHamsterSummaries();
    if (Array.isArray(summaries) && summaries.length > 0) {
      return mergeHamsterData(metadata.items, summaries);
    }
  } catch (error) {
    console.warn("Hamster summary API unavailable, fallback to static data.", error);
  }

  return metadata.items;
}

function imageHtml(src, alt) {
  if (!src) return "";
  return `<img src="${src}" alt="${alt}">`;
}

function statusList(hamster) {
  return `
    <ul class="hamster-status-list">
      <li><strong>現在：</strong>${hamster.count}匹在籍中</li>
      <li><strong>性別：</strong>オス${hamster.maleCount}匹・メス${hamster.femaleCount}匹</li>
      <li><strong>月齢：</strong>${hamster.ageRange}</li>
      <li><strong>性格：</strong>${hamster.personalityTrend}</li>
    </ul>
  `;
}

function hamsterCard(hamster) {
  return `
    <article class="hamster-card">
      <h3>${hamster.species}</h3>
      <div class="media-photo media-photo--card">
        ${imageHtml(hamster.image, hamster.species)}
      </div>
      <div class="hamster-card__body">
        ${statusList(hamster)}
        <p class="hamster-card__comment">${hamster.comment}</p>
      </div>
    </article>
  `;
}

function featuredSlide(hamster) {
  return `
    <article class="hamster-card hamster-card--featured">
      <h3>${hamster.species}</h3>
      <div class="media-photo media-photo--card">
        ${imageHtml(hamster.image, hamster.species)}
      </div>
      <div class="hamster-card__body">
        ${statusList(hamster)}
        <p class="hamster-card__comment">${hamster.comment}</p>
      </div>
    </article>
  `;
}

function chunkItems(items, size) {
  const chunks = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
}

function setupCarousel(container) {
  const track = container?.querySelector("[data-carousel-track]");
  const prev = container?.querySelector("[data-carousel-prev]");
  const next = container?.querySelector("[data-carousel-next]");
  const dotsWrap = container?.querySelector("[data-carousel-dots]");
  if (!track || !prev || !next || !dotsWrap) return;

  const slides = Array.from(track.children);
  let index = 0;

  const goTo = (nextIndex) => {
    index = Math.max(0, Math.min(nextIndex, slides.length - 1));
    track.style.transform = `translateX(-${index * 100}%)`;
    prev.disabled = index === 0;
    next.disabled = index === slides.length - 1;
    dotsWrap.querySelectorAll("button").forEach((dot, dotIndex) => {
      dot.classList.toggle("is-active", dotIndex === index);
    });
  };

  dotsWrap.innerHTML = slides
    .map(
      (_, dotIndex) =>
        `<button type="button" class="hamster-carousel__dot${dotIndex === 0 ? " is-active" : ""}" data-carousel-dot="${dotIndex}" aria-label="${dotIndex + 1}ページ目へ移動"></button>`,
    )
    .join("");

  prev.addEventListener("click", () => goTo(index - 1));
  next.addEventListener("click", () => goTo(index + 1));
  dotsWrap.addEventListener("click", (event) => {
    const button = event.target.closest("[data-carousel-dot]");
    if (!button) return;
    goTo(Number(button.dataset.carouselDot));
  });

  goTo(0);
}

async function renderFeatured() {
  const section =
    document.querySelector("[data-featured-hamsters]") ||
    document.getElementById("featured-hamsters");
  if (!section) return;
  try {
    const hamsters = await getHamsters();
    const slides = chunkItems(hamsters, 3)
      .map(
        (group) => `
          <div class="hamster-carousel__slide">
            <div class="hamster-carousel__page">
              ${group.map(featuredSlide).join("")}
            </div>
          </div>
        `,
      )
      .join("");
    section.innerHTML = `
      <div class="hamster-carousel">
        <div class="hamster-carousel__viewport">
          <div class="hamster-carousel__track" data-carousel-track>
            ${slides}
          </div>
        </div>
        <div class="hamster-carousel__controls">
          <button class="button button--ghost" type="button" data-carousel-prev>前へ</button>
          <div class="hamster-carousel__dots" data-carousel-dots></div>
          <button class="button button--ghost" type="button" data-carousel-next>次へ</button>
        </div>
      </div>
    `;
    setupCarousel(section.querySelector(".hamster-carousel"));
  } catch (error) {
    section.innerHTML = `<p class="text-soft">ハムスター情報の読み込みに失敗しました。</p>`;
  }
}

async function renderIndex() {
  const list = document.getElementById("hamster-list");
  if (!list) return;

  try {
    const hamsters = await getHamsters();
    list.innerHTML = hamsters.map(hamsterCard).join("");
  } catch (error) {
    list.innerHTML = `<p class="text-soft">ハムスター情報を表示できませんでした。</p>`;
  }
}

renderFeatured();
renderIndex();
