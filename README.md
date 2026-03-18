# はむのくらし

「はむのくらし」は、Web制作演習課題用に制作した架空サービスのサイトです。  
ハムスターカフェ、ハムスター生体販売、関連商品のEC、会員制度を組み合わせた静的Webサイトとして構成しています。

## 概要

- サイト名: はむのくらし
- コンセプト: ハムスターの幸せが、いちばんの幸せ。
- 内容:
  - ハムスターカフェ紹介
  - ハムスター一覧 / 詳細
  - 飼育用品の疑似EC
  - 会員制度紹介
- 対象:
  - 23〜29歳の独身女性
  - 小学生低学年とその保護者

## 使用技術

- HTML
- CSS
- JavaScript
- JSON
- localStorage

現時点ではバックエンドやデータベースは未実装です。  
商品、ハムスター、ニュース、景品などのデータは `assets/data/*.json` で管理しています。

## 起動方法

このサイトは `fetch` で JSON や partials を読み込むため、`file://` 直開きではなくローカルサーバー経由で確認してください。

例:

```text
http://127.0.0.1:3000/
```

トップページ:

```text
http://127.0.0.1:3000/index.html
```

## 主なページ

- トップページ: `/index.html`
- ブランド紹介: `/pages/about.html`
- カフェ: `/pages/cafe.html`
- お迎えガイド: `/pages/receive.html`
- ハムスター一覧: `/hamsters/index.html`
- 商品一覧: `/products/index.html`
- 会員制度: `/pages/membership.html`
- マイページ: `/pages/mypage.html`
- お問い合わせ: `/pages/contact.html`
- アクセス: `/pages/access.html`

## ディレクトリ構成

```text
portfolio-templates/
├ assets/
│  ├ css/
│  ├ data/
│  ├ js/
│  └ partials/
├ docs/
├ hamsters/
├ image/
├ pages/
├ products/
├ index.html
├ about.html
├ contact.html
└ README.md
```

## 主な機能

- 共通ヘッダー / フッターの読み込み
- 商品一覧 / 商品詳細の JSON 描画
- ハムスター一覧 / 詳細の JSON 描画
- localStorage を使った疑似カート
- 郵便番号から住所自動入力
- ログイン状態の保持
- マイページでのログアウト

## ドキュメント

`docs/` 配下に、課題提出用の設計資料を配置しています。

- `01-proposal.html`: 企画提案書
- `02-market-research.html`: マーケットリサーチ
- `03-persona.html`: ペルソナ
- `04-sitemap.html`: サイトマップ
- `05-wireframe.html`: ワイヤーフレーム
- `06-design-guide.html`: デザインガイド
- `07-specification.html`: 仕様書
- `08-db-design.html`: DB設計書
- `09-test-report.html`: テスト報告書
- `10-retrospective.html`: 振り返り

## 今後の予定

Web制作総合演習2では、既存の見た目をできるだけ維持したまま、以下のバックエンド機能追加を想定しています。

- Spring Boot + MyBatis
- RESTful API
- バリデーション
- Spring Security
- JUnit
- 管理画面
- お問い合わせ管理
- 将来的な作品管理CRUD

