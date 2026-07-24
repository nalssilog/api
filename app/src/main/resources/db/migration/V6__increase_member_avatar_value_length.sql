-- S3/R2 공개 URL을 저장할 수 있도록 커스텀 아바타 값의 저장 길이를 확장한다.
ALTER TABLE member
    ALTER COLUMN avatar_value TYPE character varying(500);
