# はむのくらし

ハムスターカフェ・お迎え案内・飼育用品販売・会員機能を題材にしたポートフォリオ制作物です。  
静的なフロントサイトに加えて、`spring-app/` には Spring Boot + MyBatis + H2 で構成した管理機能 / API / スタッフ向け画面を含めています。

## 概要

- フロントサイト
  - カフェ紹介
  - ハムスター一覧 / 詳細
  - お迎え案内
  - グッズ / 飼育用品販売
  - 会員登録 / ログイン / マイページ
  - お問い合わせ / アクセス / お知らせ
- バックエンド
  - 会員管理
  - お問い合わせ管理
  - ハムスター管理
  - カフェ注文 / 来店受付管理
  - 商品在庫 / 発注管理
  - シフト管理
  - スタッフチャット

## ディレクトリ構成

```text
portfolio_hum/
├─ assets/
│  ├─ css/
│  ├─ data/
│  ├─ img/
│  ├─ js/
│  └─ partials/
├─ css/
├─ data/
├─ design/
├─ docs/
├─ hamsters/
├─ image/
├─ js/
├─ pages/
├─ products/
├─ prompts/
├─ spring-app/
├─ index.html
├─ about.html
├─ works.html
├─ process.html
├─ skills.html
└─ README.md
```

## 主なページ

- トップ: `index.html`
- カフェ紹介: `pages/cafe.html`
- ハムスター一覧: `hamsters/index.html`
- ハムスター詳細: `hamsters/detail.html`
- お迎え案内: `pages/receive.html`
- 商品一覧: `products/index.html`
- 商品詳細: `products/detail.html`
- カート: `products/cart.html`
- 会員機能: `pages/membership.html`, `pages/register.html`, `pages/login.html`, `pages/mypage.html`
- お問い合わせ: `pages/contact.html`
- アクセス: `pages/access.html`
- お知らせ: `pages/news.html`

## 使用技術

### フロントエンド

- HTML
- CSS
- JavaScript
- JSON
- `fetch`
- `localStorage`

`assets/data/*.json` を読み込み、一覧表示や詳細表示を行う構成です。  
共通ヘッダー / フッターは `assets/partials/`、画面ロジックは `assets/js/` に配置しています。

### バックエンド

- Java 17
- Spring Boot 3.5.x
- Spring Web
- Spring Security
- Thymeleaf
- MyBatis
- H2 Database
- Maven

## フロントサイトの起動方法

`fetch` で JSON や partial HTML を読み込むため、`file://` ではなくローカルサーバー経由で起動してください。

例:

```powershell
cd C:\portfolio_hum
python -m http.server 3000
```

ブラウザで以下を開きます。

```text
http://127.0.0.1:3000/
```

## Spring Boot アプリの起動方法

```powershell
cd C:\portfolio_hum\spring-app
.\mvnw.cmd spring-boot:run
```

起動後の主なアクセス先:

- アプリ: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

デフォルト設定:

- JDBC URL: `jdbc:h2:file:./data/portfolio_backend;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- Username: `sa`
- Password: 空欄

## バックエンドの主な機能

- 管理画面
  - ハムスター管理
  - 会員管理
  - 商品在庫 / 発注管理
  - カフェ注文管理
  - 来店受付管理
  - お問い合わせ対応
  - シフト管理
  - 売上 / 分析画面
- スタッフ画面
  - シフト確認
  - チャット
- API
  - ハムスター
  - 会員
  - 商品
  - カフェ注文
  - お問い合わせ

## データベースについて

`spring-app/src/main/resources/schema.sql` でテーブル作成と初期データ投入を行います。  
主に以下のテーブルを扱います。

- `hamsters`
- `members`
- `employees`
- `inquiries`
- `inquiry_replies`
- `product_stocks`
- `product_orders`
- `cafe_menus`
- `cafe_orders`
- `cafe_order_items`
- `cafe_visit_sessions`
- `cafe_sales_daily`
- `shifts`
- `chat_rooms`
- `chat_messages`

## ドキュメント

制作過程や設計資料は `docs/` と `design/` にまとめています。

- `docs/01-proposal.html`: 企画書
- `docs/02-market-research.html`: 市場調査
- `docs/03-persona.html`: ペルソナ
- `docs/04-sitemap.html`: サイトマップ
- `docs/05-wireframe.html`: ワイヤーフレーム
- `docs/06-design-guide.html`: デザインガイド
- `docs/07-specification.html`: 仕様書
- `docs/08-db-design.html`: DB設計
- `docs/09-test-report.html`: テスト報告
- `docs/10-retrospective.html`: 振り返り
- `design/system-flow.html`: システムフロー
- `design/class-diagram.html`: クラス図
- `design/method-list.html`: メソッド一覧
- `design/logic-explanation.html`: ロジック説明

## 補足

- ルート配下にはポートフォリオ紹介ページ (`about.html`, `works.html`, `process.html`, `skills.html`) も含まれます。
- 既存の H2 データファイルは `data/` および `spring-app/data/` にあります。
- `spring-app-run.out` / `spring-app-run.err` は起動ログです。
