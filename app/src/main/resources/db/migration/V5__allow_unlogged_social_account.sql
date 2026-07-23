-- 추가 연동은 로그인이 아니므로 실제 로그인 전까지 last_login_at 을 비워 둔다.
ALTER TABLE social_account
    ALTER COLUMN last_login_at DROP NOT NULL;
