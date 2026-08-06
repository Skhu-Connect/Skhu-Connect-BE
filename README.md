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
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
JWT_SECRET
```

선택 환경변수:

```text
JWT_COOKIE_SECURE (기본 false)
PORT (기본 8080)
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
| 비밀번호 재설정 | `POST /connect/auth/password/reset` | 인증 토큰 | 204 |
| 학과 목록 | `GET /connect/departments` | 불필요 | 200 |
| 청원 등록·목록·상세·수정·삭제 | `/connect/petitions` | 목록·상세만 공개 | 200, 201, 204 |
| 동의 등록·취소 | `/connect/petitions/{petitionId}/agreements` | 필요 | 201, 204 |
| 북마크 등록·취소·내 목록 | `/connect/petitions/{petitionId}/bookmarks`, `/connect/petitions/bookmarks` | 필요 | 200, 201, 204 |
| 댓글·대댓글·공감 | `/connect/petitions/{petitionId}/comments/**` | 목록만 공개 | 200, 201, 204 |
| 알림 목록·미읽음·읽음 처리 | `/connect/notifications/**` | 필요 | 200, 204 |
| 내 정보·활동 내역 | `/connect/users/me/**` | 필요 | 200 |

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

## 현재 구현 범위(dev 기준)

- 학교 이메일 인증, 회원가입, BCrypt 비밀번호 저장
- JWT Access Token, Refresh Token 회전·재발급·로그아웃
- 비밀번호 재설정
- 청원 CRUD·검색·상세·논리 삭제 및 OPEN/EXPIRED 유효 상태 계산
- 동의·취소, 중복 방지, 임계치 달성 시 `UNDER_REVIEW` 전환
- 북마크 등록·취소·내 목록
- 댓글·대댓글·공감, 청원별 영구 익명 번호
- 알림 생성·목록·미읽음 개수·개별/전체 읽음 처리
- 로그인 사용자의 정보, 작성 청원·동의 청원·북마크 청원·작성 댓글·알림 조회
- Controller/MVC, Service, Entity, Repository/JPA, 인증, 동시성 및 트랜잭션 테스트

사용자 활동 조회는 JWT의 `userId`만 사용한다. 활동 목록에서는 hidden/deleted 청원과 삭제 댓글을 제외하며, 숨김 댓글은 기존 댓글 노출 정책을 재사용한다. 사용자 정보 응답은 현재 코드상 이메일, 로그인 ID, 학과 코드·이름, 알림 수신 여부를 반환하며 DB PK와 비밀번호는 반환하지 않는다.

## 테스트 현황

- 단위 테스트: Entity 불변식, Service 정책, DTO 변환, 토큰·메일 지원 로직
- MVC 테스트: Request Validation, 응답 본문과 400/403/404/409 예외 매핑, Swagger 메타데이터
- JPA 테스트: Entity 매핑, UNIQUE 제약, hidden/deleted 필터, 페이지 안정 정렬
- 통합 테스트: 회원가입·로그인/토큰 회전·비밀번호 재설정 트랜잭션, 동의·북마크·댓글 동시성
- 인증 테스트: 공개/보호 경로, JWT 누락·위조·만료·role·subject 검증
- 알림·사용자 활동 테스트: 이벤트 중복·수신 설정, 읽음 처리, JWT 사용자 범위와 활동 조회 정책

## 미구현·후속 범위

- 공식 답변 등록과 `PETITION_ANSWERED` 이벤트의 실제 호출 연결
- 관리자 인증·관리자 웹·청원 숨김 처리 API
- 알림 수신 설정 변경 API(`notification_enabled` 저장과 생성 차단 정책은 구현됨)
- 브라우저 Push 알림
- 배포 환경 확정

## 문서

- `AGENTS.md`: 작업 규칙과 Git 전략
- `ARCHITECTURE.md`: 구현 구조와 도메인 정책
- `ERD.md`: 구현 Entity와 후속 설계 Entity, 컬럼·관계·제약조건
- `AUTH_POLICY.md`: 인증 정책

## Git

- `main`: 안정 배포 기준
- `dev`: 통합 개발 기준
- 기능 브랜치는 최신 로컬 `dev`에서 생성합니다.
- 요청 없이는 commit, push, PR, merge를 수행하지 않습니다.
