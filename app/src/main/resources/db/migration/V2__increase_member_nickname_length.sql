-- 수동 입력 닉네임은 API에서 10자로 제한하지만, 가입 시 생성하는 기본 닉네임은 최대 20자까지 저장한다.
-- 기존 uk_member_nickname 유니크 제약은 컬럼 타입 변경 후에도 유지된다.
ALTER TABLE member
    ALTER COLUMN nickname TYPE character varying(20);
