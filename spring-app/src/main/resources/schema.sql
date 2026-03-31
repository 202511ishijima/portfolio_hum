CREATE TABLE IF NOT EXISTS inquiries (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	email VARCHAR(255) NOT NULL,
	subject VARCHAR(150) NOT NULL,
	message TEXT NOT NULL,
	admin_reply TEXT,
	replied_at DATETIME,
	status VARCHAR(20) NOT NULL DEFAULT 'NEW',
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

ALTER TABLE inquiries ADD COLUMN IF NOT EXISTS admin_reply TEXT;
ALTER TABLE inquiries ADD COLUMN IF NOT EXISTS replied_at DATETIME;

CREATE TABLE IF NOT EXISTS inquiry_replies (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	inquiry_id BIGINT NOT NULL,
	recipient_email VARCHAR(255) NOT NULL,
	reply TEXT NOT NULL,
	sent_at DATETIME NOT NULL,
	created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS members (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	email VARCHAR(255) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
	points INT NOT NULL DEFAULT 0,
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	email VARCHAR(255) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	position VARCHAR(100) NOT NULL,
	role VARCHAR(30) NOT NULL,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS position_permissions (
	position VARCHAR(100) PRIMARY KEY,
	default_role VARCHAR(30) NOT NULL,
	can_dashboard BOOLEAN NOT NULL DEFAULT TRUE,
	can_inquiries BOOLEAN NOT NULL DEFAULT TRUE,
	can_members BOOLEAN NOT NULL DEFAULT TRUE,
	can_employees BOOLEAN NOT NULL DEFAULT FALSE,
	can_shifts BOOLEAN NOT NULL DEFAULT TRUE,
	can_hamsters BOOLEAN NOT NULL DEFAULT TRUE,
	can_products BOOLEAN NOT NULL DEFAULT FALSE,
	can_cafe BOOLEAN NOT NULL DEFAULT FALSE,
	updated_at DATETIME NOT NULL
);

ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS default_role VARCHAR(30) NOT NULL DEFAULT 'STAFF';
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_dashboard BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_inquiries BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_members BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_employees BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_shifts BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_hamsters BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_products BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS can_cafe BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE position_permissions ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP();

MERGE INTO position_permissions KEY(position) VALUES ('本部', 'ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP());
MERGE INTO position_permissions KEY(position) VALUES ('店長', 'STAFF_MANAGER', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP());
MERGE INTO position_permissions KEY(position) VALUES ('副店長', 'STAFF_MANAGER', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP());
MERGE INTO position_permissions KEY(position) VALUES ('リーダー', 'STAFF', TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, CURRENT_TIMESTAMP());
MERGE INTO position_permissions KEY(position) VALUES ('一般従業員', 'STAFF', TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, CURRENT_TIMESTAMP());

CREATE TABLE IF NOT EXISTS shifts (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	employee_id BIGINT NOT NULL,
	work_date DATE NOT NULL,
	start_time TIME NOT NULL,
	end_time TIME NOT NULL,
	note VARCHAR(500),
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_rooms (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	room_type VARCHAR(20) NOT NULL,
	created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_messages (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	room_id BIGINT NOT NULL,
	sender_employee_id BIGINT NOT NULL,
	body TEXT NOT NULL,
	sent_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS hamsters (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	species VARCHAR(100) NOT NULL,
	sex VARCHAR(20) NOT NULL,
	birth_date DATE NOT NULL,
	health_condition VARCHAR(255) NOT NULL,
	arrival_date DATE NOT NULL,
	status VARCHAR(20) NOT NULL,
	notes TEXT,
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS product_stocks (
	product_id VARCHAR(100) PRIMARY KEY,
	stock INT NOT NULL,
	recommended_stock INT NOT NULL DEFAULT 30,
	updated_at DATETIME NOT NULL
);

ALTER TABLE product_stocks ADD COLUMN IF NOT EXISTS recommended_stock INT NOT NULL DEFAULT 30;

CREATE TABLE IF NOT EXISTS product_orders (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	product_id VARCHAR(100) NOT NULL,
	quantity INT NOT NULL,
	note VARCHAR(300),
	ordered_by VARCHAR(255) NOT NULL,
	created_at DATETIME NOT NULL
);

ALTER TABLE hamsters ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE hamsters ADD COLUMN IF NOT EXISTS health_condition VARCHAR(255) DEFAULT '店頭で個別管理';
UPDATE hamsters SET birth_date = COALESCE(birth_date, arrival_date);
UPDATE hamsters SET health_condition = COALESCE(health_condition, '店頭で個別管理');
ALTER TABLE hamsters DROP COLUMN IF EXISTS age_description;

CREATE TABLE IF NOT EXISTS cafe_menus (
	id VARCHAR(100) PRIMARY KEY,
	category VARCHAR(30) NOT NULL,
	name VARCHAR(120) NOT NULL,
	description VARCHAR(600) NOT NULL,
	price INT NOT NULL,
	image VARCHAR(255),
	available BOOLEAN NOT NULL DEFAULT TRUE,
	display_order INT NOT NULL DEFAULT 0,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS cafe_orders (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	visit_session_id BIGINT,
	seat_no VARCHAR(30) NOT NULL,
	status VARCHAR(30) NOT NULL,
	subtotal INT NOT NULL,
	tax INT NOT NULL,
	total INT NOT NULL,
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL,
	paid_at DATETIME
);

ALTER TABLE cafe_orders ADD COLUMN IF NOT EXISTS visit_session_id BIGINT;

CREATE TABLE IF NOT EXISTS cafe_order_items (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	order_id BIGINT NOT NULL,
	menu_id VARCHAR(100) NOT NULL,
	menu_name VARCHAR(120) NOT NULL,
	unit_price INT NOT NULL,
	quantity INT NOT NULL,
	line_total INT NOT NULL,
	created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS cafe_sales_daily (
	sales_date DATE PRIMARY KEY,
	order_count INT NOT NULL,
	total_amount INT NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS cafe_visit_sessions (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	session_token VARCHAR(100) NOT NULL UNIQUE,
	seat_no VARCHAR(30) NOT NULL,
	guest_count INT NOT NULL,
	status VARCHAR(30) NOT NULL,
	issued_at DATETIME NOT NULL,
	expires_at DATETIME NOT NULL,
	checkout_completed_at DATETIME,
	updated_at DATETIME NOT NULL
);

MERGE INTO cafe_menus KEY(id) VALUES ('drink-01', 'DRINK', 'ハムスター柄のモカ', 'やさしい甘さのカフェモカ。看板メニューとして人気の一杯です。', 580, 'ham_moca.png', TRUE, 10, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-02', 'DRINK', 'ミルクティー', 'まろやかな香りでゆったり楽しめる定番ドリンクです。', 560, 'Milk Tea.png', TRUE, 20, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-03', 'DRINK', 'ほうじ茶ラテ', '香ばしさとやさしい甘さを合わせた落ち着いた味わいです。', 600, 'Roasted Tea Latte.png', TRUE, 30, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-04', 'DRINK', 'キャラメルミルク', '写真映えする甘めのドリンクで、デザートとの相性も良好です。', 620, 'Caramel Milk.png', TRUE, 40, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-05', 'DRINK', 'いちごソーダ', 'くすみピンクの色合いがかわいい、季節感のあるソーダです。', 590, 'Strawberry Soda.png', TRUE, 50, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-06', 'DRINK', 'オレンジハーブティー', 'さっぱりした香りで、軽食と合わせやすい一杯です。', 540, 'Orange Herb Tea.png', TRUE, 60, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-07', 'DRINK', 'ココア', 'やさしい甘さで、寒い日にも楽しみやすい定番メニューです。', 570, 'Cocoa.png', TRUE, 70, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('drink-08', 'DRINK', '季節のレモネード', '果実感を楽しめるすっきりした味わいのドリンクです。', 610, 'Seasonal Lemonade.png', TRUE, 80, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-01', 'FOOD', 'ふわとろオムライス', 'たまごのやわらかさを楽しめる、しっかり食べたい方向けの一皿です。', 980, 'Soft Omelette Rice.png', TRUE, 110, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-02', 'FOOD', 'クリームドリア', 'まろやかなソースで食べやすい、あたたかいごはんメニューです。', 1020, 'Cream Doria.png', TRUE, 120, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-03', 'FOOD', 'きのこパスタ', '香りよく仕上げた、落ち着いた味わいのパスタです。', 960, 'Mushroom Pasta.png', TRUE, 130, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-04', 'FOOD', 'ハムスタークッキープレート', '写真を撮りたくなるかわいさを意識したデザートプレートです。', 880, 'Hamster Cookie Plate.png', TRUE, 140, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-05', 'FOOD', 'ハムスター柄のパンケーキ', '看板メニューのひとつ。ふわふわ食感と見た目のかわいさが魅力です。', 920, 'ham_pan.png', TRUE, 150, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-06', 'FOOD', 'ベジスーププレート', '軽めに食べたいときにちょうどいい、やさしい味わいのセットです。', 900, 'Veggie Soup Plate.png', TRUE, 160, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-07', 'FOOD', 'チーズトースト', 'シンプルで食べやすく、ドリンクにも合わせやすい軽食です。', 760, 'Cheese Toast.png', TRUE, 170, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('food-08', 'FOOD', 'ミニグラタン', '小腹を満たしやすい、あたたかいオーブンメニューです。', 840, 'Mini Gratin.png', TRUE, 180, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('treat-01', 'TREAT', 'ひまわりの種おためし', '定番のえさ体験を気軽に楽しめる小さめサイズです。', 150, 'Sunflower Seed Trial.png', TRUE, 210, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('treat-02', 'TREAT', 'ベジチップおためし', 'やさしい味わいのベジチップを使った体験メニューです。', 180, 'Veggie Chip Trial.png', TRUE, 220, CURRENT_TIMESTAMP());
MERGE INTO cafe_menus KEY(id) VALUES ('treat-03', 'TREAT', 'まるころスナックおためし', '写真にも映える、ころんとかわいい小さなスナックです。', 200, 'Round Snack Trial.png', TRUE, 230, CURRENT_TIMESTAMP());
