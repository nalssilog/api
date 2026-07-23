-- 과거 local 전용 시드가 법정동(B)이 아닌 행정동(H) 코드를 사용했던 두 행을
-- 실제 좌표에 해당하는 법정동 코드로 병합한다. 연결된 제보/즐겨찾기는 보존한다.
DO $$
DECLARE
    mapping record;
    old_location_id bigint;
    new_location_id bigint;
BEGIN
    FOR mapping IN
        SELECT *
        FROM (VALUES
            ('4113565000', '4113510900', '경기도', '성남시 분당구', '삼평동'),
            ('2611051000', '2611013900', '부산광역시', '중구', '남포동4가')
        ) AS mappings(old_code, new_code, sido, sigungu, dong)
    LOOP
        SELECT id
        INTO old_location_id
        FROM location
        WHERE admin_code = mapping.old_code;

        IF old_location_id IS NULL THEN
            CONTINUE;
        END IF;

        SELECT id
        INTO new_location_id
        FROM location
        WHERE admin_code = mapping.new_code;

        IF new_location_id IS NULL THEN
            UPDATE location
            SET admin_code = mapping.new_code,
                sido = mapping.sido,
                sigungu = mapping.sigungu,
                dong = mapping.dong,
                updated_at = current_timestamp
            WHERE id = old_location_id;
        ELSE
            UPDATE weather_report
            SET location_id = new_location_id
            WHERE location_id = old_location_id;

            DELETE FROM location_favorite old_favorite
            USING location_favorite new_favorite
            WHERE old_favorite.location_id = old_location_id
              AND new_favorite.location_id = new_location_id
              AND old_favorite.member_id = new_favorite.member_id;

            UPDATE location_favorite
            SET location_id = new_location_id
            WHERE location_id = old_location_id;

            DELETE FROM location
            WHERE id = old_location_id;
        END IF;
    END LOOP;
END
$$;
