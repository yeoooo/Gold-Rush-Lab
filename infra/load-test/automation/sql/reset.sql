TRUNCATE TABLE processed_mining_event, app_user, mine, mining_log
    RESTART IDENTITY
    CASCADE;
