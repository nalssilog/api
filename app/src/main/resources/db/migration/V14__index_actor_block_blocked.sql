CREATE INDEX idx_actor_block_blocked
    ON actor_block (blocked_type, blocked_key);
