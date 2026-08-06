# SKHU Connect Backend

성공회대학교 학생들이 청원을 등록하고, 동의·댓글·북마크를 통해 의견을 모으며 학교의 검토와 공식 답변까지 추적하는 학생 참여형 청원 플랫폼의 백엔드입니다.

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC, Validation, Data JPA, Mail
- Spring Security Crypto, OAuth2 JOSE(JWT)
- MySQL, Gradle Groovy
- Springdoc OpenAPI 3.0.3 / Swagger UI
- JUnit 5, Mockito, Spring Boot Test

## 구현 완료(dev 기준)

- 공통 JPA·환경변수·Swagger 기반
- 학과 도메인과 학과 목록 조회
- 사용자 도메인, 학교 이메일 인증, 회원가입, 로그인
- JWT Access Token, Refresh Token 재발급·회전, 로그아웃
- 비밀번호 재설정
- 임계치 기본 도메인
- 청원 등록·수정·논리 삭제·목록·검색·상세 조회
- 청원 동의·취소, 목표 달성 시 `UNDER_REVIEW` 전환
- 청원 북마크 등록·취소·내 목록
- 댓글·대댓글·댓글 공감, 청원별 익명 번호

## 진행 중

브랜치 `feat/19-user-notification`에서 사용자 알림 문서와 구현을 작업 중입니다. 알림 Entity/API와 주요 이벤트 연결 코드가 작업 트리에 있으나 전체 `clean test`, `clean build`가 완료되기 전까지 완료 기능으로 간주하지 않습니다. 공식 답변 등록 API가 아직 없어 `ANSWERED` 알림의 실제 호출 지점도 연결되지 않았습니다.

## 남은 주요 작업

- 사용자 알림 구현 검증 및 `ANSWERED` 이벤트 연결
- 공식 답변·관리자 웹 기능
- 사용자 정보·활동 내역 조회
- 사용자 웹 API 통합 테스트와 문서 최종 정리
- 배포 환경 확정

## 실행

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
JWT_COOKIE_SECURE (선택, 기본 false)
PORT (선택, 기본 8080)
```

Windows:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat clean test
.\gradlew.bat clean build
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 문서 읽기 순서

1. `AGENTS.md` — 작업 규칙, Git 전략, 현재 상태
2. `ARCHITECTURE.md` — 구조와 확정 정책
3. `ERD.md` — 테이블·관계·제약조건
4. 관련 소스와 테스트

## Git

- `main`: 안정 배포 기준
- `dev`: 통합 개발 기준
- 기능 브랜치: 최신 로컬 `dev`에서 생성
- 요청 없이는 commit, push, PR, merge를 수행하지 않습니다.