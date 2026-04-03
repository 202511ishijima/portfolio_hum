# はむのくらし（Portfolio）

ハムスターカフェを題材にした学習用ポートフォリオです。  
フロントは静的ページ、バックエンドは `spring-app/` の Spring Boot + MyBatis + H2 で構成しています。

## 主な機能

### フロントエンド
- トップページ、ハムスター紹介、商品一覧
- 会員ページ（ログイン/マイページ）
- お問い合わせページ
- カフェ注文専用ページ（セッショントークン付きURL）

### バックエンド（管理画面）
- 管理者ログイン
- お問い合わせ管理
- 会員管理
- 従業員管理（役職/権限）
- シフト管理（希望入力/月間表/自動作成）
- ハムスター管理
- 商品在庫管理
- カフェ受付発行/注文一覧/メニュー価格設定

## ディレクトリ（主要）

```text
portfolio-templates/
├─ assets/
├─ pages/
├─ hamsters/
├─ products/
├─ docs/
├─ design/
├─ spring-app/
└─ README.md
```

## 動作環境

- Java 17
- Maven Wrapper（`spring-app/mvnw.cmd`）
- VS Code Live Server（推奨）

## 起動方法

### 1. フロント（推奨: Live Server）

VS Codeで `index.html` を開き、`Open with Live Server` を実行してください。  
通常は `http://127.0.0.1:3000` などで確認できます。

補足: Python がある場合は `python -m http.server 3000` でも配信可能ですが、このリポジトリは Live Server 運用を前提にしています。

### 2. バックエンド

```powershell
cd c:\academia\portfolio-templates\spring-app
.\mvnw.cmd spring-boot:run
```

## 主要URL

### フロント
- トップ: `http://127.0.0.1:3000/index.html`
- ハムスター一覧: `http://127.0.0.1:3000/hamsters/index.html`
- 商品一覧: `http://127.0.0.1:3000/products/index.html`
- お問い合わせ: `http://127.0.0.1:3000/pages/contact.html`
- カフェ注文専用: `http://127.0.0.1:3000/pages/cafe-order.html?session=<token>`

### バックエンド
- アプリ: `http://localhost:8080`
- 管理ログイン: `http://localhost:8080/admin/login`
- ダッシュボード: `http://localhost:8080/admin/dashboard`
- シフト管理: `http://localhost:8080/admin/shifts`
- カフェ受付発行: `http://localhost:8080/admin/cafe/reception`
- H2 Console: `http://localhost:8080/h2-console`

## DB情報（H2）

- JDBC URL: `jdbc:h2:file:./data/portfolio_backend;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- User: `sa`
- Password: 空

`spring-app/src/main/resources/schema.sql` でテーブル作成・差分反映を行います。

## 注意

- `*.mv.db` はローカル開発用データです。
- GitHub Pages は静的ファイル配信のみのため、Spring Boot側のDB保存/API機能は反映されません。
