-- 한 글자 검색은 prefix 인덱스로, 두 글자 이상의 부분 검색은 trigram 인덱스로 처리한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_location_sido_prefix
    ON location (sido text_pattern_ops);

CREATE INDEX idx_location_sigungu_prefix
    ON location (sigungu text_pattern_ops);

CREATE INDEX idx_location_dong_prefix
    ON location (dong text_pattern_ops);

CREATE INDEX idx_location_label_prefix
    ON location ((sido || ' ' || sigungu || ' ' || dong) text_pattern_ops);

CREATE INDEX idx_location_label_trgm
    ON location USING gin ((sido || ' ' || sigungu || ' ' || dong) gin_trgm_ops);
