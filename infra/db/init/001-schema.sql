-- =========================================================

-- Gold Rush v0.1

-- PostgreSQL 16

-- =========================================================

CREATE TABLE mine
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    remaining_amount BIGINT      NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version          BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_mine
        PRIMARY KEY (id),

    CONSTRAINT chk_mine_remaining_amount
        CHECK (remaining_amount >= 0)
);

CREATE TABLE app_user
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    mine_id          BIGINT NOT NULL,
    total_mined_gold BIGINT      NOT NULL DEFAULT 0,
    session_id       UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_user
        PRIMARY KEY (id),
    CONSTRAINT uk_app_user_session_id
        UNIQUE (session_id),
    CONSTRAINT fk_app_user_mine
        FOREIGN KEY (mine_id)
            REFERENCES mine (id),
    CONSTRAINT chk_app_user_total_mined_gold
        CHECK (total_mined_gold >= 0)
);

CREATE TABLE mining_log
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id    BIGINT      NOT NULL,
    mine_id    BIGINT      NOT NULL,
    amount     BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mining_log
        PRIMARY KEY (id),
    CONSTRAINT fk_mining_log_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (id),
    CONSTRAINT fk_mining_log_mine
        FOREIGN KEY (mine_id)
            REFERENCES mine (id),
    CONSTRAINT chk_mining_log_amount
        CHECK (amount > 0)
);