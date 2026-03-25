async function getHamsters() {
  const basePath =
    window.SiteRouter && typeof window.SiteRouter.getBasePath === "function"
      ? window.SiteRouter.getBasePath()
      : "";
  const response = await fetch(`${basePath}assets/data/hamsters.json`);
  if (!response.ok) throw new Error("Failed to fetch hamsters");
  const hamsters = await response.json();
  return hamsters.map((hamster) => ({
    ...hamster,
    image: hamster.image.replace("../", basePath),
  }));
}

function imageHtml(src, alt) {
  if (!src) return "";
  return `<img src="${src}" alt="${alt}">`;
}

function statusList(hamster) {
  return `
    <ul class="hamster-status-list">
      <li><strong>\u73fe\u5728\uff1a</strong>${hamster.count}\u5339\u5728\u7c4d\u4e2d</li>
      <li><strong>\u6027\u5225\uff1a</strong>\u30aa\u30b9${hamster.maleCount}\u5339\u30fb\u30e1\u30b9${hamster.femaleCount}\u5339</li>
      <li><strong>\u6708\u9f62\uff1a</strong>${hamster.ageRange}</li>
      <li><strong>\u6027\u683c\uff1a</strong>${hamster.personalityTrend}</li>
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
        `<button type="button" class="hamster-carousel__dot${dotIndex === 0 ? " is-active" : ""}" data-carousel-dot="${dotIndex}" aria-label="${dotIndex + 1}\u30da\u30fc\u30b8\u76ee\u3078\u79fb\u52d5"></button>`,
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
          <button class="button button--ghost" type="button" data-carousel-prev>\u524d\u3078</button>
          <div class="hamster-carousel__dots" data-carousel-dots></div>
          <button class="button button--ghost" type="button" data-carousel-next>\u6b21\u3078</button>
        </div>
      </div>
    `;
    setupCarousel(section.querySelector(".hamster-carousel"));
  } catch (error) {
    section.innerHTML = `<p class="text-soft">\u30cf\u30e0\u30b9\u30bf\u30fc\u60c5\u5831\u306e\u8aad\u307f\u8fbc\u307f\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002</p>`;
  }
}

async function renderIndex() {
  const list = document.getElementById("hamster-list");
  if (!list) return;

  try {
    const hamsters = await getHamsters();
    list.innerHTML = hamsters.map(hamsterCard).join("");
  } catch (error) {
    list.innerHTML = `<p class="text-soft">\u30cf\u30e0\u30b9\u30bf\u30fc\u60c5\u5831\u3092\u8868\u793a\u3067\u304d\u307e\u305b\u3093\u3067\u3057\u305f\u3002</p>`;
  }
}

renderFeatured();
renderIndex();
