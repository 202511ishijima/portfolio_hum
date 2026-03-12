//  プロジェクトアーカイブ 共通JS

// ナビゲーション生成関数
function generateNavigation() {
  const sidebar = document.querySelector('.sidebar');
  if (!sidebar) return;

  // data-nav-level属性でパス階層を判定（root, docs, design, prompts）
  const navLevel = sidebar.dataset.navLevel || 'root';

  // パスプレフィックスを設定
  let rootPath = './';
  let docsPath = './docs/';
  let designPath = './design/';
  let promptsPath = './prompts/';

  if (navLevel === 'docs') {
    rootPath = '../';
    docsPath = './';
    designPath = '../design/';
    promptsPath = '../prompts/';
  } else if (navLevel === 'design') {
    rootPath = '../';
    docsPath = '../docs/';
    designPath = './';
    promptsPath = '../prompts/';
  } else if (navLevel === 'prompts') {
    rootPath = '../';
    docsPath = '../docs/';
    designPath = '../design/';
    promptsPath = './';
  }

  const navHTML = `
    <div class="sidebar-header">
      <div class="logo">PROJECT ARCHIVE</div>
      <div class="project-name"><br>プロジェクトアーカイブ</div>
    </div>
    <nav class="sidebar-nav">
      <div class="nav-group">
        <div class="nav-group-title">プロジェクト</div>
        <a href="${rootPath}index.html"><span class="material-symbols-outlined icon-sm">home</span> トップページ</a>
        <a href="${rootPath}about.html"><span class="material-symbols-outlined icon-sm">person</span> 自己紹介</a>
        <a href="${rootPath}works.html"><span class="material-symbols-outlined icon-sm">work</span> 制作物一覧</a>
        <a href="${rootPath}process.html"><span class="material-symbols-outlined icon-sm">assignment</span> 制作プロセス</a>
        <a href="${rootPath}skills.html"><span class="material-symbols-outlined icon-sm">bolt</span> スキルシート</a>
        <a href="${rootPath}contact.html"><span class="material-symbols-outlined icon-sm">mail</span> お問い合わせ</a>
      </div>
      <div class="nav-group">
        <div class="nav-group-title">制作ドキュメント</div>
        <a href="${docsPath}01-proposal.html"><span class="nav-number">01</span> 企画提案書</a>
        <a href="${docsPath}02-market-research.html"><span class="nav-number">02</span> マーケットリサーチ</a>
        <a href="${docsPath}03-persona.html"><span class="nav-number">03</span> ペルソナシート</a>
        <a href="${docsPath}04-sitemap.html"><span class="nav-number">04</span> サイトマップ</a>
        <a href="${docsPath}05-wireframe.html"><span class="nav-number">05</span> ワイヤーフレーム</a>
        <a href="${docsPath}06-design-guide.html"><span class="nav-number">06</span> デザインガイドライン</a>
        <a href="${docsPath}10-retrospective.html"><span class="nav-number">10</span> 振り返り・技術記事</a>
      </div>
      <div class="nav-group">
        <div class="nav-group-title">設計資料</div>
        <a href="${docsPath}07-specification.html"><span class="nav-number">07</span> 仕様書</a>
        <a href="${docsPath}08-db-design.html"><span class="nav-number">08</span> DB設計書</a>
        <a href="${docsPath}09-test-report.html"><span class="nav-number">09</span> テスト報告書</a>
        <a href="${designPath}system-flow.html"><span class="material-symbols-outlined icon-sm">account_tree</span> システムフロー図</a>
        <a href="${designPath}class-diagram.html"><span class="material-symbols-outlined icon-sm">lan</span> クラス構成図</a>
        <a href="${designPath}method-list.html"><span class="material-symbols-outlined icon-sm">list_alt</span> メソッド一覧</a>
        <a href="${designPath}logic-explanation.html"><span class="material-symbols-outlined icon-sm">search</span> ロジック解説</a>
      </div>
      <div class="nav-group">
        <div class="nav-group-title">プロンプト</div>
        <a href="${promptsPath}prompt-step.html"><span class="material-symbols-outlined icon-sm">format_list_numbered</span> ステップ別プロンプト</a>
        <a href="${promptsPath}prompt-function.html"><span class="material-symbols-outlined icon-sm">extension</span> 機能追加別プロンプト</a>
        <a href="${promptsPath}prompt-log.html"><span class="material-symbols-outlined icon-sm">history</span> 実行ログ</a>
      </div>
    </nav>
    <div class="sidebar-footer">
      &copy; Project Archive 2026
    </div>
  `;

  sidebar.innerHTML = navHTML;
}

document.addEventListener('DOMContentLoaded', () => {
  // ナビゲーションを生成
  generateNavigation();

  // Sidebar toggle for mobile
  const hamburger = document.querySelector('.hamburger');
  const sidebar = document.querySelector('.sidebar');

  if (hamburger && sidebar) {
    hamburger.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });

    // Close sidebar when clicking outside
    document.addEventListener('click', (e) => {
      if (sidebar.classList.contains('open') &&
          !sidebar.contains(e.target) &&
          !hamburger.contains(e.target)) {
        sidebar.classList.remove('open');
      }
    });

    // Close sidebar when clicking a nav link (mobile)
    sidebar.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        if (window.innerWidth <= 768) {
          sidebar.classList.remove('open');
        }
      });
    });
  }

  // Set active nav item based on current page
  const currentPage = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.sidebar-nav a').forEach(link => {
    const href = link.getAttribute('href');
    if (href && (href.endsWith(currentPage) || (currentPage === 'index.html' && href === './index.html'))) {
      link.classList.add('active');
    }
  });

  // Copy prompt text functionality
  document.querySelectorAll('.prompt-box').forEach(box => {
    const promptText = box.querySelector('.prompt-text');
    if (promptText) {
      promptText.style.cursor = 'pointer';
      promptText.title = 'クリックしてコピー';

      promptText.addEventListener('click', () => {
        const text = promptText.textContent;
        navigator.clipboard.writeText(text).then(() => {
          const originalBg = promptText.style.background;
          promptText.style.background = '#d1fae5';
          promptText.style.transition = 'background 0.3s';
          setTimeout(() => {
            promptText.style.background = originalBg || '#fff';
          }, 500);
        });
      });
    }
  });

  // Smooth scroll for TOC links
  document.querySelectorAll('.toc-list a').forEach(link => {
    link.addEventListener('click', (e) => {
      const href = link.getAttribute('href');
      if (href && href.startsWith('#')) {
        e.preventDefault();
        const target = document.querySelector(href);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }
    });
  });
});
