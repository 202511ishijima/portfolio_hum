window.SiteRouter = {
  getBasePath() {
    return document.body.dataset.basePath || "./";
  },

  getQueryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
  },

  async fetchJSON(relativePath) {
    const response = await fetch(`${this.getBasePath()}assets/data/${relativePath}`);
    if (!response.ok) {
      throw new Error(`Failed to load ${relativePath}`);
    }
    return response.json();
  },

  formatPrice(value) {
    return `¥${Number(value).toLocaleString("ja-JP")}`;
  }
};
