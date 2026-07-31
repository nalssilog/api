ALTER TABLE social_account
    DROP CONSTRAINT social_account_provider_check;

ALTER TABLE social_account
    ADD CONSTRAINT social_account_provider_check
        CHECK (provider IN ('GOOGLE', 'KAKAO', 'NAVER', 'APPLE'));
