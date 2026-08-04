# SKHU Connect Backend Architecture

> Last Updated: 2026-08-04
>
> 본 문서는 SKHU Connect 백엔드 개발의 공식 설계 문서이다.
> 모든 개발(Codex 포함)은 이 문서를 기준으로 진행한다.

---

# 1. 프로젝트 목표

SKHU Connect는 성공회대학교 학생들의 의견을 학교와 연결하는 청원 플랫폼이다.

현재 MVP에서는 다음 기능을 구현한다.

- 사용자 웹
- 관리자 웹
- Railway 배포

---

# 2. 프로젝트 구조

Spring Boot 프로젝트는 하나만 사용한다.

프로젝트 내부를 기능별로 분리한다.

```
org.skhuconnect

├── global
├── auth
├── user
├── department
├── petition
├── agreement
├── bookmark
├── comment
├── notification
├── admin
```

추후 모바일 앱 API가 생기면

```
app
```

패키지를 추가한다.

---

# 3. API Prefix

사용자

```
/connect
```

관리자

```
/connect/admin
```

api/v1 형태는 사용하지 않는다.

---

# 4. 인증 방식

일반 사용자 인증은 Access Token과 Refresh Token을 함께 사용한다.

```text
Access Token
+
Refresh Token
```

## Access Token

- HS256으로 서명한 JWT이다.
- 만료시간은 30분이다.
- `sub` claim은 User ID 문자열이다.
- `role` claim은 `USER`이다.
- 서명 키는 `JWT_SECRET` 환경변수에서 읽는다.
- `JWT_SECRET`은 Base64 값이어야 하며 디코딩 결과가 최소 32바이트여야 한다.

전달 방식:

```http
Authorization: Bearer {AccessToken}
```

## Refresh Token

- `SecureRandom`으로 생성한 256비트 opaque token이다.
- 유효기간은 14일이다.
- 원문은 데이터베이스에 저장하지 않고 SHA-256 `token_hash`만 저장한다.
- 사용자당 활성 Refresh Token은 최대 하나이다.
- 로그인 시 기존 활성 토큰을 교체한다.
- 재발급 시 기존 토큰을 회전하여 즉시 사용할 수 없게 한다.
- 로그아웃 시 활성 토큰을 삭제한다.
- 활성 행에서 조회되지 않는 토큰은 `TOKEN_INVALID`로 처리한다.
- 활성 행에서 만료가 확인된 토큰은 `TOKEN_EXPIRED`로 처리한다.
- Refresh Token 조회와 회전에는 비관적 락을 사용한다.
- Redis는 MVP에서 사용하지 않는다.

Cookie 정책:

- 이름: `refreshToken`
- `HttpOnly`
- `Path=/connect/auth`
- `SameSite=Lax`
- `Max-Age=1209600`
- `Secure`는 `JWT_COOKIE_SECURE` 환경변수로 설정한다.

## 현재 보안 구현 범위

- 일반 사용자 로그인·재발급·로그아웃 및 JWT 발급은 구현되어 있다.
- JWT 지원에는 `spring-security-oauth2-jose`를 사용한다.
- Spring Security 전체 필터 체인과 Access Token 인증 필터는 아직 구현하지 않았다.
## 이메일 인증 저장

- 이메일 인증 상태는 기존 MySQL에 EmailVerification Entity로 저장한다.
- 이메일과 인증 목적별 하나의 활성 레코드를 유지한다.
- 인증 목적은 SIGN_UP, PASSWORD_RESET으로 구분한다.
- 인증번호 원문은 저장하지 않고 random salt를 포함한 SHA-256 해시만 저장한다.
- 인증 성공 시 일회용 verificationToken을 발급하며 원문은 클라이언트에 한 번만 반환한다.
- verificationToken 원문은 저장하지 않고 SHA-256 해시만 저장한다.
- 이메일 인증 저장 및 만료 관리에 Redis를 사용하지 않는다.

---

# 5. 회원가입

회원가입 순서

학교 이메일 인증

↓

loginId 생성

↓

비밀번호 생성

↓

학과 선택

↓

회원가입 완료

학교 이메일은

```
@office.skhu.ac.kr
```

만 허용한다.

학교 계정 비밀번호는 저장하지 않는다.

---

# 6. 로그인

로그인은

```
loginId
password
```

사용한다.

학교 이메일은 로그인에 사용하지 않는다.

구현된 일반 사용자 인증 API:

```http
POST /connect/auth/login
POST /connect/auth/token/refresh
POST /connect/auth/logout
```

---

# 7. 비밀번호

Spring Security BCrypt 사용

---

# 8. 권한

권한은 두 개만 존재한다.

```
USER
ADMIN
```

User와 Admin Entity는 분리한다.

Admin은 관리자 로그인만 담당한다.

---

# 9. BaseEntity

모든 Entity는 BaseEntity를 상속한다.

공통 컬럼

```
createdAt

updatedAt
```

---

# 10. Entity

현재 MVP Entity

```
User

Admin

Department

Petition

Agreement

Bookmark

Comment

CommentLike

Notification

OfficialAnswer

ThresholdSetting

NotificationLog

RefreshToken

EmailVerification
```

History Entity는 MVP 이후 구현한다.

---

# 11. Entity 관계

Department

```
1 : N User
```

User

```
1 : N Petition

1 : N Agreement

1 : N Bookmark

1 : N Comment

1 : N CommentLike

1 : N Notification
```

Petition

```
1 : N Agreement

1 : N Bookmark

1 : N Comment

1 : 1 OfficialAnswer
```

Comment

```
1 : N CommentLike
```

Admin

```
1 : N OfficialAnswer

1 : N NotificationLog
```
RefreshToken

```
1 : 1 User
```

OfficialAnswer

```
N : 1 Admin

1 : 1 Petition
```

Notification

```
N : 1 User
```

Agreement

```
N : 1 User

N : 1 Petition
```

Bookmark

```
N : 1 User

N : 1 Petition
```

Comment

```
N : 1 User

N : 1 Petition
```

CommentLike

```
N : 1 User

N : 1 Comment
```
---

# 12. Petition Category

Enum

```
SCHOLARSHIP

FACILITY

DORMITORY

LIBRARY

DEPARTMENT
```

현재는

학부 카테고리만 존재한다.

특정 학부 대상 청원은 MVP에서 구현하지 않는다.

추후 targetDepartment 컬럼을 추가하여 확장한다.

---

# 13. Petition Status

```
OPEN

UNDER_REVIEW

ANSWERED

EXPIRED
```

---

# 14. 삭제 정책

관리자

```
숨김(hidden)
```

사용자

```
삭제(deleted)
```

숨김과 삭제는 서로 다른 개념이다.

---

# 15. 댓글 정책

댓글 작성자는

```
익명1

익명2

...
```

형태로 표시한다.

청원 작성자가 댓글을 작성하면

```
익명(작성자)
```

로 표시한다.

실제 User 정보는 절대 반환하지 않는다.

---

# 16. OfficialAnswer

청원 하나당 답변 하나만 존재한다.

Petition : OfficialAnswer

```
1 : 1
```

---

# 17. Threshold

ThresholdSetting Entity에서 관리한다.

관리자가 수정 가능하다.

카테고리별 임계치를 저장한다.

---

# 18. Notification

사용자 알림은 Notification Entity에서 관리한다.

웹 푸시는 MVP에서 제외한다.

---

# 19. JWT 정책

- Access Token은 HS256 JWT이고 유효기간은 30분이다.
- Access Token의 `sub`는 User ID 문자열이고 `role`은 `USER`이다.
- Access Token은 데이터베이스에 저장하지 않는다.
- Refresh Token은 256비트 opaque token이고 유효기간은 14일이다.
- Refresh Token 원문 대신 SHA-256 해시를 `RefreshToken` Entity에 저장한다.
- Refresh Token은 `refreshToken` HttpOnly Cookie로 전달한다.
- 로그인 시 교체하고 재발급 시 회전하며 로그아웃 시 삭제한다.
- Spring Security 전체 필터 체인과 Access Token 인증 필터는 아직 구현하지 않았다.

---
# 20. 개발 원칙

유지보수성을 가장 우선한다.

Entity는 기능별로 분리한다.

불필요한 복잡성은 추가하지 않는다.

확장 가능한 구조를 우선한다.

MVP 이후 기능은 TODO로 남긴다.

---

# 21. Git 전략

브랜치

```
main
dev

feat/#이슈번호-기능명
```

기능 단위로 브랜치를 생성한다.

---

# 22. GitHub Issue

기능별 Issue 생성 후 개발한다.

Issue 하나당 기능 하나를 구현한다.

---

# 23. Commit Message

모든 Commit Message는 반드시 한국어를 사용한다.

예시

```
feat: 사용자 엔티티 추가

feat: 청원 작성 API 구현

fix: 댓글 공감 중복 수정

refactor: 청원 서비스 구조 개선
```

영문 Commit Message는 사용하지 않는다.

Unicode Escape(\uXXXX) 형태도 사용하지 않는다.

실제 한글 문자열을 사용한다.

---

# 24. 개발 우선순위

1. Entity

2. Repository

3. DTO

4. Service

5. Controller

6. Test

7. Swagger

8. Railway 배포

---

# 25. 구현 원칙

Codex는 반드시

Issue

↓

설계 확인

↓

Entity

↓

Repository

↓

DTO

↓

Service

↓

Controller

↓

Test

순서대로 구현한다.

구조를 임의로 변경하지 않는다.

ARCHITECTURE.md를 최우선 기준으로 개발한다.

---

# 26. 패키지 구조 규칙

모든 도메인은 동일한 패키지 구조를 따른다.

예시

```
user

├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
```

global 패키지는 다음 구조를 사용한다.

```
global

├── config
├── exception
├── response
├── security
└── util
```

---

# 27. Entity 설계 규칙

모든 Entity는 다음 규칙을 따른다.

- Setter를 사용하지 않는다.
- 생성자는 Builder 또는 생성 메서드를 사용한다.
- 비즈니스 로직은 Entity 내부 메서드로 관리한다.
- 모든 Entity는 BaseEntity를 상속한다.

---

# 28. DTO 규칙

Controller는 Entity를 직접 반환하지 않는다.

모든 요청과 응답은 DTO를 사용한다.

Entity는 Controller 계층 밖으로 노출하지 않는다.

Request DTO와 Response DTO를 분리한다.

---

# 29. Service 규칙

비즈니스 로직은 Service에서만 수행한다.

Controller는 요청과 응답만 처리한다.

Repository는 데이터 조회 및 저장만 담당한다.

---

# 30. 문서 변경 원칙

ARCHITECTURE.md는 프로젝트의 공식 설계 문서이다.

구조(Entity, 인증, 패키지, API Prefix, 개발 규칙)를 변경하는 경우 반드시 먼저 ARCHITECTURE.md를 수정한 후 구현을 진행한다.

구현이 문서를 앞서지 않는다.

---

# 31. 구현 체크리스트

새로운 기능을 구현할 때는 반드시 아래 순서를 따른다.

1. ARCHITECTURE.md 확인
2. GitHub Issue 확인
3. Entity 설계 확인
4. 구현
5. Test
6. Commit
---

# 32. 구현 금지 사항

다음 사항은 구현하지 않는다.

- Controller에서 Repository 직접 호출
- Entity를 API Response로 직접 반환
- Setter 기반 Entity 수정
- 비즈니스 로직을 Controller에 작성
- Repository에 비즈니스 로직 작성
- 구조를 ARCHITECTURE.md와 다르게 변경

구현 완료 후에는 반드시 빌드 및 테스트를 수행한다.
