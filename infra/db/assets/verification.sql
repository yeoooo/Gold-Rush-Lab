-- DB 초기화
TRUNCATE app_user CASCADE ;
TRUNCATE mine CASCADE ;
TRUNCATE mining_log CASCADE ;

ALTER SEQUENCE app_user_id_seq RESTART WITH 1;
ALTER SEQUENCE mine_id_seq RESTART WITH 1;
ALTER SEQUENCE mining_log_id_seq RESTART WITH 1;

-- 통합
WITH w_m AS (
    SELECT m.id AS mine_id, COUNT(m.id) * 100 AS initial_remaining, m.created_at
    FROM mine m
             JOIN app_user au ON m.id = au.mine_id
    GROUP BY m.id
)
SELECT wm.mine_id AS "광산_ID"
     , wm.initial_remaining "초기_잔량"
     , SUM(ml.amount) AS "실제_채굴량"
     , wm.initial_remaining - SUM(ml.amount) AS "실제_잔량"
     , wm.created_at AS "광산_생성일"
FROM w_m wm
         JOIN mining_log ml
              ON wm.mine_id = ml.mine_id
GROUP BY wm.mine_id, wm.created_at, wm.initial_remaining;

-- 실제 채굴량
SELECT COUNT(id)
FROM mining_log
WHERE mine_id = 1
;

-- 광산 잔량 확인
SELECT id AS "광산_ID", remaining_amount AS "잔량", created_at AS "생성 시간"
FROM mine
;

-- 지정
WITH w_target AS (
    SELECT 1 AS mine_id
),
     w_m AS (
         SELECT
             m.id AS mine_id,
             COUNT(au.id) * 100 AS initial_remaining,
             m.created_at
         FROM mine m
                  JOIN w_target wt
                       ON wt.mine_id = m.id
                  JOIN app_user au
                       ON au.mine_id = m.id
         GROUP BY m.id, m.created_at
     )
SELECT
    wm.mine_id AS "광산_ID",
    wm.initial_remaining AS "초기_잔량",
    COALESCE(SUM(ml.amount), 0) AS "실제_채굴량",
    wm.initial_remaining - COALESCE(SUM(ml.amount), 0) AS "실제_잔량",
    wm.created_at AS "광산_생성일"
FROM w_m wm
         LEFT JOIN mining_log ml
                   ON ml.mine_id = wm.mine_id
GROUP BY
    wm.mine_id,
    wm.created_at,
    wm.initial_remaining;
