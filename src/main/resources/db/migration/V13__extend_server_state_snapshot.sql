ALTER TABLE server_state
    ADD COLUMN player_count INTEGER,
    ADD COLUMN max_players INTEGER,
    ADD COLUMN game_mode VARCHAR(255);
