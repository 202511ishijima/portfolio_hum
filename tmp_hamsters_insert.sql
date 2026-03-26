INSERT INTO hamsters (species, sex, birth_date, health_condition, arrival_date, status, notes, created_at, updated_at)
VALUES
('ゴールデンハムスター', 'MALE', DATE '2026-02-10', '店頭で個別管理', DATE '2026-03-20', 'AVAILABLE', 'ダミー登録: おっとりした子', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('ゴールデンハムスター', 'FEMALE', DATE '2026-02-18', '店頭で個別管理', DATE '2026-03-20', 'NEGOTIATING', 'ダミー登録: 見学相談あり', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('ジャンガリアンハムスター', 'MALE', DATE '2026-02-22', '店頭で個別管理', DATE '2026-03-21', 'AVAILABLE', 'ダミー登録: 元気な子', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('ジャンガリアンハムスター', 'FEMALE', DATE '2026-02-25', '店頭で個別管理', DATE '2026-03-21', 'AVAILABLE', 'ダミー登録: 人慣れしやすい子', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('ロボロフスキーハムスター', 'MALE', DATE '2026-01-30', '店頭で個別管理', DATE '2026-03-22', 'AVAILABLE', 'ダミー登録: すばしっこい子', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('キャンベルハムスター', 'FEMALE', DATE '2026-02-05', '店頭で個別管理', DATE '2026-03-22', 'AVAILABLE', 'ダミー登録: 落ち着いた子', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

SELECT id, species, sex, birth_date, arrival_date, status FROM hamsters ORDER BY id DESC LIMIT 10;
