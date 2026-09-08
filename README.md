# SKHU Connect Backend

성공회대학교 학생이 학교 이메일로 인증하고 청원을 등록하며, 동의·북마크·익명 댓글로 의견을 모으는 학생 참여형 청원 플랫폼의 백엔드입니다.

## 기술 스택

- Java 17, Spring Boot 4.1.0, Gradle Groovy
- Spring Web MVC, Validation, Data JPA, Mail
- Spring Security Crypto, OAuth2 JOSE(JWT)
- MySQL, Hibernate(`ddl-auto=update`, Open Session in View 비활성화)
- Springdoc OpenAPI 3.0.3 / Swagger UI
- JUnit 5, Mockito, Spring Boot Test

## 실행 방법

필수 환경변수:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
RESEND_API_KEY
MAIL_FROM
JWT_SECRET
```

선택 환경변수:

```text
JWT_COOKIE_SECURE (기본 false)
PORT (기본 8080)
FIREBASE_SERVICE_ACCOUNT_JSON (미설정 시 FCM 푸시 발송만 비활성화)
```

`JWT_SECRET`은 Base64로 인코딩된 256-bit 이상의 HS256 키여야 합니다. 비밀값은 저장소에 기록하지 않습니다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat clean test
.\gradlew.bat clean build
```

## API 문서

애플리케이션 실행 후 Swagger UI에서 실제 Request/Response, Validation, 응답 상태를 확인합니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- 인증 API를 호출할 때 Swagger `Authorize`에 Access Token을 입력합니다.

### 구현 API

| 기능 | 경로 | 인증 | 성공 상태 |
|---|---|---:|---:|
| 이메일 인증 발송·확인 | `POST /connect/auth/email-verifications`, `/confirm` | 불필요 | 204, 200 |
| 회원가입 | `POST /connect/auth/signup` | 불필요 | 201 |
| 로그인·재발급·로그아웃 | `POST /connect/auth/login`, `/token/refresh`, `/logout` | Refresh는 Cookie | 200, 204 |
| 아이디 찾기 | `POST /connect/auth/login-id/find/email`, `/password` | 불필요 | 200 |
| 비밀번호 재설정 | `POST /connect/auth/password/reset` | 인증 토큰 | 204 |
| 학과 목록 | `GET /connect/departments` | 불필요 | 200 |
| 청원 등록·목록·상세·수정·삭제 | `/connect/petitions` | 목록·상세만 공개 | 200, 201, 204 |
| 동의 등록·취소 | `/connect/petitions/{petitionId}/agreements` | 필요 | 201, 204 |
| 북마크 등록·취소·내 목록 | `/connect/petitions/{petitionId}/bookmarks`, `/connect/petitions/bookmarks` | 필요 | 200, 201, 204 |
| 댓글·대댓글·공감 | `/connect/petitions/{petitionId}/comments/**` | 목록만 공개 | 200, 201, 204 |
| 알림 목록·미읽음·읽음 처리 | `/connect/notifications/**` | 필요 | 200, 204 |
| 공지사항 조회·배너 닫기 | `/connect/notices`, `/connect/users/me/notices/**` | 전체 목록 공개, 닫기 필요 | 200, 204 |
| 내 정보·활동 내역·알림 설정 | `/connect/users/me/**` | 필요 | 200 |
| 내 아이디·비밀번호 변경 | `PATCH /connect/users/me/login-id`, `/password` | 필요 | 200, 204 |
| 사용자 영구 차단 | `POST /connect/users/me/blocks` | 필요 | 201 |

공통 오류 응답은 Problem Detail 형식이다. 요청 검증 실패는 400, Access Token 누락·위조·만료·잘못된 role은 401, 작성자 권한 위반은 403, 존재하지 않거나 사용자에게 노출할 수 없는 데이터는 404, 중복 참여 또는 허용되지 않는 상태 충돌은 409를 사용한다. 인증 도메인의 만료 상태는 해당 Controller의 Swagger 명세를 따른다.

목록 API는 기본 `page=0`, `size=20`을 사용한다. 사용자 활동과 알림은 `size=1..100`이며 `createdAt DESC, id DESC`로 안정 정렬한다. 청원 목록의 허용 정렬값과 검색 조건은 Swagger 명세를 따른다.

## 프로젝트 구조

```text
org.skhuconnect
├─ auth              이메일 인증·회원가입·로그인·토큰·비밀번호 재설정
├─ user              사용자 Entity와 본인 정보·활동 조회
├─ department        학과
├─ petition          청원
├─ agreement         동의
├─ bookmark          북마크
├─ comment           댓글·대댓글·공감·익명 번호
├─ notification      사용자 알림
├─ threshold         청원 임계치
└─ global            공통 설정·보안·예외·JPA 기반
```

## 현재 구현 범위

- 인증: 학교 이메일 인증, 회원가입, 로그인, JWT/Refresh Token, 아이디 찾기, 비밀번호 재설정·변경, 회원 탈퇴와 30일 재가입 제한
- 청원: CRUD, 검색·상세, 동의, 북마크, 댓글·대댓글·공감, 청원별 익명 번호, 10분 작성 쿨다운, 공유 공개 조회
- 알림·공지: 사용자 알림, 종류별 수신 설정, FCM 푸시 연동, 공지사항과 사용자별 배너 닫기
- 사용자: 내 정보와 작성·동의·북마크·댓글·알림 활동 조회, 사용자 영구 차단
- 관리자: 관리자 인증, 임계치 관리, 콘텐츠 숨김·복구, 공식 답변, 신고 처리, 운영 로그와 대시보드
- 테스트: Controller/MVC, Service, Entity, Repository/JPA, 인증, 동시성 및 트랜잭션 테스트

## 테스트 현황

- 단위·MVC·JPA·통합 테스트로 주요 도메인 정책, 인증, 예외 매핑, UNIQUE 제약, 동시성, 알림·사용자 활동 범위를 검증한다.
- 최종 변경 검증은 `.\gradlew.bat clean test`, `.\gradlew.bat clean build`, `git diff --check`를 기준으로 한다.

## 미구현·후속 범위

- 브라우저 Push 알림

## 배포

- `main` 브랜치 푸시 시 GitHub Actions가 지인 서버에 SSH로 자동 배포한다.
- 워크플로우: `.github/workflows/deploy.yml`
- 서버: `i1000u@i1000u-ssh.hueeng.com:22022`
- 작업 디렉터리: `/home/i1000u/skhu-connect`
- 서비스명: `skhu-connect`
- Health check: `https://i1000u.hueeng.com/actuator/health`

```bash
ssh -p 22022 -i "/c/Users/grand/Desktop/대학 파일/성공잇다/i1000u_ssh.key" i1000u@i1000u-ssh.hueeng.com
```

## 문서

- `AGENTS.md`: 작업 규칙과 Git 전략
- `ARCHITECTURE.md`: 구현 구조와 도메인 정책
- `ERD.md`: 구현 Entity와 후속 설계 Entity, 컬럼·관계·제약조건
- `PRIVACY_POLICY.md`: 현재 코드 기준 개인정보 처리 및 App Store 개인정보 분류
- `AUTH_POLICY.md`: 인증 정책

## Git

- `main`: 안정 배포 기준
- `dev`: 통합 개발 기준
- 기능 브랜치는 최신 로컬 `dev`에서 생성합니다.
- 요청 없이는 commit, push, PR, merge를 수행하지 않습니다.
