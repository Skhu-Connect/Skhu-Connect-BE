-- 2026-10-26 운영 SHOW CREATE TABLE 확인 결과에 맞춘 배포 전 마이그레이션.
-- 먼저 기존 스키마를 백업한다. 이 SQL은 최초 적용용이며 재실행 전 스키마를 확인한다.
-- 각 ALTER의 DROP/ADD는 같은 문장에서 실행해 기존 제약을 유지한 채 허용값만 확장한다.
ALTER TABLE petitions
    DROP CHECK petitions_chk_1,
    ADD CONSTRAINT petitions_chk_1 CHECK (category IN (
        'SCHOLARSHIP', 'FACILITY', 'DORMITORY', 'LIBRARY', 'DEPARTMENT', 'SOFTWARE_CONVERGENCE'
    ));

ALTER TABLE threshold_settings
    DROP CHECK threshold_settings_chk_1,
    ADD CONSTRAINT threshold_settings_chk_1 CHECK (category IN (
        'SCHOLARSHIP', 'FACILITY', 'DORMITORY', 'LIBRARY', 'DEPARTMENT', 'SOFTWARE_CONVERGENCE'
    ));

ALTER TABLE admins ADD COLUMN role VARCHAR(30) NULL;
UPDATE admins SET role = 'STUDENT_COUNCIL' WHERE role IS NULL;

-- 비대위 계정은 새 백엔드 배포·권한 확인 후 별도 생성한다.
-- 비밀번호·해시·토큰은 이 파일에 기록하지 않는다.
-- SOFTWARE_CONVERGENCE 임계치는 비대위의 최초 PUT 요청 값으로 생성한다.
