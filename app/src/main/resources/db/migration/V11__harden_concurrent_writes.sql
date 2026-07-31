ALTER TABLE social_account
    ADD CONSTRAINT uk_social_account_member_provider
        UNIQUE (member_id, provider);

ALTER TABLE popular_location_snapshot
    ADD CONSTRAINT uk_popular_location_snapshot_calculation
        UNIQUE (
            calculated_at,
            window_started_at,
            ranking_limit,
            algorithm_version
        );

CREATE TABLE popular_location_snapshot_lock (
    id bigint PRIMARY KEY
);

INSERT INTO popular_location_snapshot_lock (id)
VALUES (1);
