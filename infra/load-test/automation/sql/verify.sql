WITH user_total AS (
    SELECT mine_id, COALESCE(SUM(total_mined_gold), 0) AS amount
    FROM app_user
    WHERE mine_id = :'mine_id'::bigint
    GROUP BY mine_id
),
log_total AS (
    SELECT mine_id, COALESCE(SUM(amount), 0) AS amount
    FROM mining_log
    WHERE mine_id = :'mine_id'::bigint
    GROUP BY mine_id
)
SELECT
    :'initial_amount'::bigint,
    COALESCE(user_total.amount, 0),
    COALESCE(log_total.amount, 0),
    mine.remaining_amount,
    (
        :'initial_amount'::bigint
            = COALESCE(user_total.amount, 0) + mine.remaining_amount
        AND :'initial_amount'::bigint
            = COALESCE(log_total.amount, 0) + mine.remaining_amount
        AND COALESCE(user_total.amount, 0) = COALESCE(log_total.amount, 0)
    )
FROM mine
LEFT JOIN user_total ON user_total.mine_id = mine.id
LEFT JOIN log_total ON log_total.mine_id = mine.id
WHERE mine.id = :'mine_id'::bigint;
