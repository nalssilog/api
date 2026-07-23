-- 전국 법정동 검색 데이터는 좌표를 제공하지 않는다.
-- 좌표 기반 법정동 조회는 카카오 API가 담당하므로, 사전 적재 행의 좌표는 NULL을 허용한다.
ALTER TABLE location ALTER COLUMN latitude DROP NOT NULL;
ALTER TABLE location ALTER COLUMN longitude DROP NOT NULL;

-- 같은 데이터셋을 애플리케이션 시작 때마다 다시 적재하지 않기 위한 버전 기록.
CREATE TABLE location_dataset_import (
    dataset_version character varying(20) PRIMARY KEY,
    source_name character varying(200) NOT NULL,
    imported_at timestamp(6) with time zone NOT NULL
);
