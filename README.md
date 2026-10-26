<div align="center">

<img src="https://raw.githubusercontent.com/Skhu-Connect/.github/main/profile/assets/logo.png" width="96" alt="성공잇다" />

# 성공잇다 백엔드

성공회대학교 학생들이 익명으로 건의하고, 요청을 모아 학교의 답변을 확인하는 서비스입니다.
이 저장소는 백엔드 API 기능 구현을 담당합니다.

([스웨거주소](https://i1000u.hueeng.com/swagger-ui/index.html))

## 주요 기능

학교 이메일로 재학생 인증을 마친 뒤 익명으로 건의를 등록합니다. 요청이 카테고리별 기준에 도달하면 검토를 거쳐 공식 답변을 확인할 수 있습니다.

| 구분 | 기능 |
| --- | --- |
| 학생 웹 | 이메일 인증, 익명 건의, 검색·필터, 요청·댓글, 북마크·공유, 유사 건의 찾기 |
| 관리자 웹 | 대시보드, 건의·신고 관리, 공식 답변, 카테고리별 도달 기준 설정, 공지사항 관리 |
| iOS 앱 | 건의 탐색·등록·요청, 댓글, 내 활동, 알림 설정, 푸시 알림 |

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 웹 | Java 17·SpringBoot·JPA·MySQL. JWT·BCrypt 인증|


## 개발 담당

| 담당 | 이름 |
| --- | --- |
| 프론트엔드 | 김석환([별도 저장소](https://github.com/Skhu-Connect/Skhu-Connect-FE)) |
| 백엔드 | 전천우  |

## 소프트웨어융합학부 시범운영

- `SOFTWARE_CONVERGENCE` 카테고리를 추가한다. 현재 학과 코드가 `SOFTWARE_CONVERGENCE`인 학생만 이 카테고리를 작성할 수 있다.
- 회원가입과 마이페이지 학부 변경은 동일한 `users.department_id`를 사용한다. 변경 후 카테고리 목록 재조회 시 재로그인 없이 반영된다.
- `GET /connect/petitions/categories`는 USER 인증이 필요한 작성 가능 카테고리 문자열 배열이다. 피드 조회·검색·필터는 전체 카테고리를 유지한다.
- 관리자 DB 역할은 `STUDENT_COUNCIL`(기존 카테고리와 공지 관리), `EMERGENCY_COMMITTEE`(소프트웨어 카테고리 관리)로 구분한다. 전체 카테고리 조회는 양쪽 모두 가능하다. JWT 역할과 기존 로그인 API는 `ADMIN`을 유지한다.
- `GET /connect/admin/me`는 `{id, loginId, role}`을 반환하며, 신고 응답에는 `petitionCategory`가 추가된다. 관리자 권한 위반은 403, title=`ADMIN_FORBIDDEN`이다.
- 새 카테고리의 임계치 행은 비대위의 최초 PUT으로 생성하고 이후 동일 API로 수정한다.

배포 전 실제 운영 CHECK 제약 확장과 `admins.role` 컬럼 추가가 필요하다. 확인된 스키마에 대한 SQL은 `scripts/20261026_software_category.sql`에 있으며, 기존 데이터를 보존한다. 관리자 계정의 비밀번호는 BCrypt 해시만 저장한다.

## 로컬 실행과 검증

필수 환경변수는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RESEND_API_KEY`, `MAIL_FROM`, `JWT_SECRET`이다. 비밀값은 저장소에 기록하지 않는다. 선택 환경변수와 상세 정책은 `ARCHITECTURE.md` 및 실행 설정을 따른다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat clean test
.\gradlew.bat clean build
```

Swagger: `/swagger-ui/index.html`, OpenAPI: `/v3/api-docs`.

상세 API 계약·Swagger 확인 순서·파일별 변경은 `SOFTWARE_ADMIN_TEST_GUIDE.md`, 관리자 권한 정책은 `ADMIN_POLICY.md`, 스키마와 제약 주의사항은 `ERD.md`를 참고한다.
