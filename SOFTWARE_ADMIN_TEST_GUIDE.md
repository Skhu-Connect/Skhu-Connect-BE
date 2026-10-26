# 소프트웨어융합학부 시범운영 백엔드 로컬 테스트

## 1. 작업 범위

- 로컬 dev의 `317e3fa`에서 생성한 `feat/software-council-admin` 브랜치다.
- 기존 API·로그인·JWT role=ADMIN·Refresh Token·쿠키·응답 형식을 유지한다.
- 프런트, 운영 데이터, 배포 설정은 변경하지 않는다. commit·push·PR·merge·배포는 수행하지 않는다.
- 새 카테고리 값은 `SOFTWARE_CONVERGENCE`, 표시 이름은 소프트웨어융합학부다.

## 2. 권한

| 기능 | 총학생회 (`STUDENT_COUNCIL` 또는 기존 NULL) | 소프트웨어 비대위 (`EMERGENCY_COMMITTEE`) |
|---|---|---|
| 전체 카테고리 글·댓글·답변·설정·신고 조회 | 가능 | 가능 |
| 기존 카테고리 답변·숨김·복구·임계치·신고 처리 | 가능 | 403 |
| 소프트웨어 카테고리 답변·숨김·복구·임계치·신고 처리 | 403 | 가능 |
| 공지 작성·수정·발행·숨김·재공개 | 가능 | 403 |
| 신고의 사용자 로그인 정지 조치 | 기존 카테고리 신고에서 가능 | 소프트웨어 카테고리 신고에서 가능 |

권한은 토큰의 추가 claim이 아니라 요청 시 DB의 관리자 소속과 대상 카테고리를 검사한다. 관리자 소속 변경 API·공개 관리자 회원가입은 추가하지 않는다.

## 3. DB 준비 (로컬에서만)

애플리케이션이 사용하는 DB가 로컬 DB인지 먼저 확인한다. 기존 실행 환경변수 설정은 README.md를 따른다. 운영용 접속정보를 복사하지 않는다.

`admins.role VARCHAR(30) NULL` 컬럼이 추가된다. NULL은 기존 관리자 호환을 위한 총학생회 값이다. FK·UNIQUE·인덱스 변경이나 기존 데이터 삭제는 없다.

기존 방식대로 준비한 관리자 계정 중 비대위 계정의 `role`을 `EMERGENCY_COMMITTEE`로 설정한다. 기존 학생회 계정은 `UPDATE admins SET role = 'STUDENT_COUNCIL' WHERE role IS NULL;`로 채운다. 마이그레이션 전 NULL도 기존 학생회 권한으로 해석한다. 이 SQL은 실행하지 않았으며 로컬 DB를 확인한 뒤 적용한다. 비밀번호는 BCrypt 해시를 사용한다. 로그인은 양쪽 모두 기존 `POST /connect/admin/auth/login`이다.

`threshold_settings`에는 새 카테고리 설정 row가 필요하다. 비대위의 최초 `PUT /connect/admin/threshold-settings/SOFTWARE_CONVERGENCE`는 row가 없으면 요청 값으로 생성한다(200). 이후 같은 PUT은 수정한다. 따라서 배포 때 임의의 임시값을 넣을 필요는 없으며 비대위가 콘솔에서 최초 값을 입력하면 된다. 학생회는 최초 생성도 403이고, 기존 카테고리의 없는 설정은 계속 404다. 설정을 최초 생성하기 전 해당 카테고리 청원 작성은 404다.

동시 최초 생성은 기존 category UNIQUE로 중복을 막고, 확인된 해당 UNIQUE 충돌만 409(`Threshold setting already exists`)로 반환한다. 다시 조회·PUT하면 된다. CHECK 위반 등 다른 스키마 오류를 중복으로 간주해 삼키지 않는다.

| 컬럼 | 값 |
|---|---|
| category | SOFTWARE_CONVERGENCE |
| total_student_count | 소프트웨어융합학부 인원 (1 이상) |
| threshold_rate | 동의 비율 (0.0001~1.0000) |
| minimum_count | 최소 동의 인원 (1 이상) |
| created_at, updated_at | 초기 설정 시각 |

사용자 학부는 `GET /connect/departments`로 확인한다. 소프트웨어융합학부의 학과 코드는 운영 공개 학과 API에서 확인한 `SOFTWARE_CONVERGENCE`다. 실제 로컬 데이터가 이 코드인지 확인해야 한다. Department 새 테이블이나 별도 사용자 학부 필드는 추가하지 않는다.

### 기존 DB enum 제약

Petition.category와 ThresholdSetting.category의 기존 `@Enumerated` + VARCHAR 매핑을 유지한다. 이미 만들어진 DB의 CHECK/ENUM 제약은 `ddl-auto=update`로 확장되지 않을 수 있다. 운영 제약은 미확인이며 운영 DB에 대한 변경은 실행하지 않았다.

로컬 기존 DB 또는 향후 배포 대상에서는 `SHOW CREATE TABLE petitions;`, `SHOW CREATE TABLE threshold_settings;`로 실제 category 제약을 확인해야 한다. 기존 제약에 새 값이 빠졌으면 실제 제약 이름과 전체 기존 값 목록을 확인한 뒤 별도 마이그레이션으로 새 값을 포함한다. 확인하지 않은 제약을 자동으로 삭제하거나 운영에 적용하지 않는다. 자세한 장애 이력은 ERD.md 26절을 따른다.

## 4. Swagger 테스트 순서

1. 로컬 앱 실행 후 `/swagger-ui/index.html`을 연다. 각 관리자 로그인 토큰을 Swagger Authorize에 번갈아 입력한다.
2. `GET /connect/admin/me`: `{ "id": 1, "loginId": "council", "role": "STUDENT_COUNCIL" }` 형식인지 확인한다. 비대위 role은 `EMERGENCY_COMMITTEE`여야 한다. JWT의 role은 기존처럼 ADMIN이다.
3. 양쪽 관리자 토큰으로 기존 `GET /connect/admin/petitions` 및 댓글·답변 조회를 호출한다. 양쪽 카테고리를 모두 조회할 수 있어야 한다.
4. 카테고리를 교차해 청원·댓글 숨김/복구와 공식 답변 등록/수정을 호출한다. 담당 카테고리만 성공하고 나머지는 Problem Detail 403, title=`ADMIN_FORBIDDEN`이어야 한다. 공식 답변 등록은 기존처럼 UNDER_REVIEW 청원에서 가능하다.
5. 비대위의 `PUT /connect/admin/threshold-settings/SOFTWARE_CONVERGENCE`로 최초 설정을 생성한 뒤 같은 PUT으로 수정한다. 담당 카테고리만 성공하고 변경 사유·관리자가 기록돼야 한다. 기존 청원의 targetAgreementCount는 유지돼야 한다.
6. 비대위 토큰으로 모든 공지 변경 API를 호출하면 403이어야 한다. 신고 HIDE/기각/USER_LOGIN_BAN은 모두 담당 카테고리만 가능하다. 청원·댓글 신고 응답의 petitionCategory가 실제 소속 청원의 카테고리인지 확인한다.
7. 일반 USER 토큰으로 `GET /connect/petitions/categories`를 호출한다. 현재 users.department_id의 학과 코드가 SOFTWARE_CONVERGENCE일 때만 SOFTWARE_CONVERGENCE가 목록에 포함돼야 한다. 비로그인 요청은 401이다.
8. 같은 USER 토큰으로 `PATCH /connect/users/me/department`를 호출해 SOFTWARE_CONVERGENCE 학부로 변경하고 7번을 재조회한다. 재로그인 없이 새 카테고리가 포함돼야 한다. 다른 학부로 변경하면 다시 제외돼야 한다.
9. 다른 학부 사용자가 SOFTWARE_CONVERGENCE를 직접 지정해 청원을 등록하면 403이어야 한다. 소프트웨어 학부는 기존 쿨다운·검증·임계치 정책 안에서 등록할 수 있다. 다른 카테고리 작성·공개 조회·동의 정책은 유지한다.

## 5. 프런트 연결 범위

작성 선택창은 `GET /connect/petitions/categories`의 최신 응답을 사용해야 한다. 학부 변경 후 다시 조회하면 된다. 일반 청원 조회용 카테고리 필터는 전체 카테고리를 유지한다. 프런트는 이번 작업에서 수정하지 않았으므로 선택창 숨김의 실제 화면 반영은 이 API 연결 이후다.

학부는 기존과 같이 사용자가 직접 선택·변경한 DB 값 기준이다. 학적 시스템을 통한 실제 소속 검증은 이번 범위에 추가하지 않는다.

### 프런트 작성 카테고리 계약

`GET /connect/petitions/categories`는 USER Bearer Token이 필수인 enum 문자열 배열이다. 소프트웨어 학부는 아래 응답을 받고, 다른 학부는 마지막 SOFTWARE_CONVERGENCE만 제외된다.

```json
["SCHOLARSHIP", "FACILITY", "DORMITORY", "LIBRARY", "DEPARTMENT", "SOFTWARE_CONVERGENCE"]
```

비로그인은 401, title=`Unauthorized`다. 다른 학부의 SOFTWARE_CONVERGENCE 직접 등록은 403, title=`Petition category is not available for your department`다. 회원가입·마이페이지 변경이 함께 사용하는 users.department_id의 현재 값을 조회하므로 같은 토큰으로도 학부 변경 후 목록 재조회 시 반영된다. 피드 조회·검색·카테고리 필터에는 학부 제한을 걸지 않는다. 기존 숨김·삭제·사용자 차단 필터는 유지한다.

## 6. 변경 파일 보고

시작 상태는 main, 미추적 presentation-demo/다. 로컬 dev에서 기능 브랜치만 생성했고 기존 미추적 폴더는 보존했다. 의존성 추가와 파일 삭제는 없다.

| 구분 | 파일 | 핵심 변경 |
|---|---|---|
| 수정 | `ADMIN_POLICY.md` | 관리자 역할별 조회·변경·공지·신고 정책과 프런트 계약 |
| 수정 | `ARCHITECTURE.md` | 관리자 변경 권한 및 최신 사용자 학부 기준 작성 정책 |
| 수정 | `ERD.md` | admins.role 컬럼, 새 카테고리, 임계치 최초 생성 및 DB 영향 |
| 수정 | `README.md` | 새 API와 시범운영 기능, 테스트 가이드 링크 |
| 생성 | `SOFTWARE_ADMIN_TEST_GUIDE.md` | 로컬 설정, API 계약, Swagger 순서와 파일별 변경 보고 |
| 수정 | `src/main/java/org/skhuconnect/admin/answer/controller/AdminOfficialAnswerController.java` | 공식 답변 권한 Swagger와 기존 깨진 설명 정리 |
| 수정 | `src/main/java/org/skhuconnect/admin/answer/service/AdminOfficialAnswerService.java` | 공식 답변 등록·수정 권한 검사 |
| 수정 | `src/main/java/org/skhuconnect/admin/content/controller/AdminContentController.java` | 관리자 변경 권한 403 Swagger 명세 |
| 수정 | `src/main/java/org/skhuconnect/admin/content/service/AdminContentService.java` | 청원·댓글·대댓글 숨김·복구 전에 카테고리 권한 검사 |
| 생성 | `src/main/java/org/skhuconnect/admin/controller/AdminManagementController.java` | 인증된 GET /connect/admin/me와 Swagger |
| 생성 | `src/main/java/org/skhuconnect/admin/dto/AdminManagementResponse.java` | 프런트 계약 id·loginId·role 응답 |
| 수정 | `src/main/java/org/skhuconnect/admin/entity/Admin.java` | String role 매핑, 기존 NULL 호환과 비대위 계정 생성 팩터리 |
| 생성 | `src/main/java/org/skhuconnect/admin/exception/AdminManagementException.java` | 관리 권한 위반 예외 |
| 생성 | `src/main/java/org/skhuconnect/admin/exception/AdminManagementExceptionHandler.java` | 403 ProblemDetail와 ADMIN_FORBIDDEN title |
| 생성 | `src/main/java/org/skhuconnect/admin/service/AdminManagementPermission.java` | 역할별 카테고리 변경·공지 권한을 한 곳에서 검사 |
| 생성 | `src/main/java/org/skhuconnect/admin/service/AdminManagementService.java` | DB 관리자 조회와 자기 프로필 응답 구성 |
| 수정 | `src/main/java/org/skhuconnect/admin/threshold/controller/AdminThresholdSettingController.java` | 최초 생성 동작과 403·409 Swagger 명세 |
| 수정 | `src/main/java/org/skhuconnect/admin/threshold/exception/AdminThresholdSettingException.java` | 동시 최초 생성 UNIQUE 충돌 사유 |
| 수정 | `src/main/java/org/skhuconnect/admin/threshold/exception/AdminThresholdSettingExceptionHandler.java` | 동시 생성 충돌 409 매핑 |
| 수정 | `src/main/java/org/skhuconnect/admin/threshold/service/AdminThresholdSettingService.java` | 권한 검사, 새 카테고리 최초 PUT 생성, UNIQUE 충돌만 409 처리 |
| 수정 | `src/main/java/org/skhuconnect/admin/token/exception/AdminAuthExceptionHandler.java` | 관리자 프로필의 없는 관리자 토큰 오류를 기존 401 처리에 연결 |
| 수정 | `src/main/java/org/skhuconnect/notice/controller/NoticeController.java` | 공지 수정·숨김·재공개에 인증 adminId 전달과 403 명세 |
| 수정 | `src/main/java/org/skhuconnect/notice/service/NoticeService.java` | 공지의 모든 변경 동작에 학생회 권한 검사 |
| 수정 | `src/main/java/org/skhuconnect/petition/controller/PetitionController.java` | 작성 카테고리 조회 API와 등록 제한 Swagger |
| 수정 | `src/main/java/org/skhuconnect/petition/entity/PetitionCategory.java` | SOFTWARE_CONVERGENCE enum 값 추가 |
| 수정 | `src/main/java/org/skhuconnect/petition/exception/PetitionException.java` | 학부별 카테고리 작성 제한 오류 사유 |
| 수정 | `src/main/java/org/skhuconnect/petition/exception/PetitionExceptionHandler.java` | 학부별 카테고리 작성 제한 403과 title |
| 수정 | `src/main/java/org/skhuconnect/petition/service/PetitionService.java` | 최신 학부 기준 작성 카테고리 목록과 등록 제한 |
| 수정 | `src/main/java/org/skhuconnect/report/controller/ReportController.java` | 관리자 신고 처리 권한 403 Swagger 명세 |
| 수정 | `src/main/java/org/skhuconnect/report/dto/ReportResponse.java` | 청원·댓글 신고 모두 petitionCategory 추가 |
| 수정 | `src/main/java/org/skhuconnect/report/repository/ReportRepository.java` | 댓글 신고 목록에서 comment.petition을 함께 조회 |
| 수정 | `src/main/java/org/skhuconnect/report/service/ReportService.java` | 신고 대상 청원 카테고리로 모든 신고 처리 권한 검사 |
| 생성 | `src/test/java/org/skhuconnect/admin/AdminManagementIntegrationTest.java` | 실제 JWT·MVC·JPA로 역할·공지·답변·콘텐츠·신고·초기 임계치 검증 |
| 수정 | `src/test/java/org/skhuconnect/admin/answer/service/AdminOfficialAnswerServiceTest.java` | 권한이 있는 실제 Admin 테스트 fixture 적용 |
| 수정 | `src/test/java/org/skhuconnect/admin/content/service/AdminContentServiceTest.java` | 권한이 있는 실제 Admin 테스트 fixture 적용 |
| 생성 | `src/test/java/org/skhuconnect/admin/service/AdminManagementPermissionTest.java` | 기존 NULL·미등록 역할·상호 카테고리·공지 권한 검증 |
| 수정 | `src/test/java/org/skhuconnect/admin/threshold/service/AdminThresholdSettingServiceTest.java` | 실제 Admin fixture와 UNIQUE 충돌·CHECK 위반 구분 검증 |
| 수정 | `src/test/java/org/skhuconnect/notice/service/NoticeServiceTest.java` | 실제 학생회 Admin fixture와 인증 ID 전달 변경 반영 |
| 생성 | `src/test/java/org/skhuconnect/petition/SoftwareCategoryIntegrationTest.java` | 학부 변경·같은 토큰·선택 목록·등록 제한·Swagger 검증 |
| 수정 | `src/test/java/org/skhuconnect/report/service/ReportServiceTest.java` | 실제 학생회 Admin fixture와 신고 대상 카테고리 지정 |

## 7. 최종 검증 결과 (2026-10-26)

| 검증 | 실제 결과 |
|---|---|
| 변경 전 clean test | 430개 성공 |
| 최종 clean test | BUILD SUCCESSFUL |
| 최종 clean build | BUILD SUCCESSFUL, check·build·bootJar 완료 |
| 빌드의 JUnit 결과 | 115 suites, 452 tests, failures=0, errors=0, skipped=0 |
| git diff --check | 통과 |
| git diff --name-only / git status --short | 실제 확인, 아래 상태 기록 |

검증은 저장소의 테스트용 H2 환경에서 실행했다. 기존 MySQL 데이터베이스의 category CHECK/ENUM 제약과 실제 학과 코드는 별도 확인 대상이다. 운영 DB 제약 조회는 자동 승인 검토에서 서비스 환경파일 자격증명 사용 접근에 명시적 승인이 없다는 이유로 거절되어 실행하지 않았다. 진행 상태 진단 명령도 별도 승인 검토 시간 초과로 미실행됐지만, 실행 중이던 Gradle 세션에서 최종 빌드 성공을 확인했다.

의존성 추가·파일 삭제는 없고, 신규 10개·수정 30개 파일은 미커밋이다. 기존 미추적 presentation-demo/는 보존했다. commit·push·PR·merge·배포·운영 DB 변경은 수행하지 않았다. 실제 로컬 MySQL 서버 실행이나 사용자 계정·학과·임계치 데이터 초기화는 실행하지 않았으며, Swagger 수동 테스트는 이 가이드의 로컬 준비 후 수행한다. 프런트는 변경하지 않았다.

### 최종 git status --short

```text
 M ADMIN_POLICY.md
 M ARCHITECTURE.md
 M ERD.md
 M README.md
 M src/main/java/org/skhuconnect/admin/answer/controller/AdminOfficialAnswerController.java
 M src/main/java/org/skhuconnect/admin/answer/service/AdminOfficialAnswerService.java
 M src/main/java/org/skhuconnect/admin/content/controller/AdminContentController.java
 M src/main/java/org/skhuconnect/admin/content/service/AdminContentService.java
 M src/main/java/org/skhuconnect/admin/entity/Admin.java
 M src/main/java/org/skhuconnect/admin/threshold/controller/AdminThresholdSettingController.java
 M src/main/java/org/skhuconnect/admin/threshold/exception/AdminThresholdSettingException.java
 M src/main/java/org/skhuconnect/admin/threshold/exception/AdminThresholdSettingExceptionHandler.java
 M src/main/java/org/skhuconnect/admin/threshold/service/AdminThresholdSettingService.java
 M src/main/java/org/skhuconnect/admin/token/exception/AdminAuthExceptionHandler.java
 M src/main/java/org/skhuconnect/notice/controller/NoticeController.java
 M src/main/java/org/skhuconnect/notice/service/NoticeService.java
 M src/main/java/org/skhuconnect/petition/controller/PetitionController.java
 M src/main/java/org/skhuconnect/petition/entity/PetitionCategory.java
 M src/main/java/org/skhuconnect/petition/exception/PetitionException.java
 M src/main/java/org/skhuconnect/petition/exception/PetitionExceptionHandler.java
 M src/main/java/org/skhuconnect/petition/service/PetitionService.java
 M src/main/java/org/skhuconnect/report/controller/ReportController.java
 M src/main/java/org/skhuconnect/report/dto/ReportResponse.java
 M src/main/java/org/skhuconnect/report/repository/ReportRepository.java
 M src/main/java/org/skhuconnect/report/service/ReportService.java
 M src/test/java/org/skhuconnect/admin/answer/service/AdminOfficialAnswerServiceTest.java
 M src/test/java/org/skhuconnect/admin/content/service/AdminContentServiceTest.java
 M src/test/java/org/skhuconnect/admin/threshold/service/AdminThresholdSettingServiceTest.java
 M src/test/java/org/skhuconnect/notice/service/NoticeServiceTest.java
 M src/test/java/org/skhuconnect/report/service/ReportServiceTest.java
?? SOFTWARE_ADMIN_TEST_GUIDE.md
?? presentation-demo/
?? src/main/java/org/skhuconnect/admin/controller/
?? src/main/java/org/skhuconnect/admin/dto/
?? src/main/java/org/skhuconnect/admin/exception/
?? src/main/java/org/skhuconnect/admin/service/
?? src/test/java/org/skhuconnect/admin/AdminManagementIntegrationTest.java
?? src/test/java/org/skhuconnect/admin/service/
?? src/test/java/org/skhuconnect/petition/SoftwareCategoryIntegrationTest.java
```


## 8. 배포 준비 (2026-10-26, 사용자 별도 승인 후)

7절은 로컬 구현 완료 시점의 기록이다. 이후 사용자가 운영 배포와 비대위 계정 생성을 요청했다. 비밀번호는 코드·문서에 기록하지 않는다.

sudo를 통한 기존 서비스 환경파일 사용과 읽기 전용 스키마 조회를 명시적으로 승인받아 운영 스키마를 확인했다. petitions_chk_1과 threshold_settings_chk_1은 기존 카테고리 5개만 허용하며, admins.role은 아직 없다. 배포 전 SQL은 scripts/20261026_software_category.sql이다. 기존 값·FK·UNIQUE를 유지하며 CHECK 허용값 확장, nullable role 컬럼 추가, 기존 NULL 역할의 STUDENT_COUNCIL 채우기만 수행한다.

최신 원격 main의 README 디자인과 기존 안내는 보존하고 새 기능 설명을 덧붙였다. main의 기존 SSH 자동 배포 워크플로우를 사용한다. 비대위 계정은 새 권한 코드 배포 후 생성하며, 임계치는 비대위가 최초 PUT으로 입력한다. 관리자 화면은 같은 백엔드와 기존 API 경로를 사용하므로 별도 백엔드 URI는 필요 없다. 새 프런트 도메인 CORS는 주소를 받은 후 별도 추가한다.

실제 배포 결과는 배포 완료 시점의 채팅 보고와 GitHub Actions 결과를 기준으로 확인한다.


### 운영 확인 후 학과 코드 보완

2026-10-26 운영 GET /connect/departments에서 소프트웨어융합학부의 실제 code가 SOFTWARE_CONVERGENCE임을 확인했다. 기존 로컬 fixture의 SOFTWARE 가정 대신 이 운영 코드를 사용하도록 Service·Swagger·통합 테스트를 맞췄다. 사용자 FK나 학과 데이터를 변경하지 않는다. 학부 변경 후 동일 토큰으로 작성 목록 포함·제외와 직접 등록 허용·차단을 같은 통합 테스트로 검증한다.
