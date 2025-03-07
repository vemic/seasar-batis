INSERT INTO sbtest_users (
    sequence_no,
    amount,
    rate,
    score,
    is_active,
    name,
    description,
    memo,
    char_code,
    created_at,
    updated_at,
    birth_date,
    work_time,
    status,
    user_type,
    preferences
) VALUES 
-- テストユーザー1: findByPk, findAll, where, 複雑なクエリの対象
(
    1,
    2000.00,
    0.05,
    85.0,
    1,
    'テストユーザー1',
    '一般ユーザー1の説明',
    'メモ1',
    'JP1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_DATE,
    '09:00:00',
    'ACTIVE',
    'USER',
    '{"lang": "ja", "theme": "dark"}'
),
-- テストユーザー2: findAll の対象
(
    2,
    1500.00,
    0.03,
    70.0,
    0,
    'テストユーザー2',
    '管理者ユーザーの説明',
    'メモ2',
    'JP2',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_DATE,
    '10:00:00',
    'INACTIVE',
    'ADMIN,USER',
    '{"lang": "en", "theme": "light"}'
),
-- 複雑なクエリ用: ステータス条件、score > 80またはuser_type=VIPかつpreferences存在
(
    3,
    3000.00,
    0.04,
    90.0,
    1,
    'テストユーザー3',
    '複雑クエリ用のユーザー',
    'メモ3',
    'JP3',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_DATE,
    '11:00:00',
    'ACTIVE',
    'VIP',
    '{"lang": "ja", "theme": "system"}'
);