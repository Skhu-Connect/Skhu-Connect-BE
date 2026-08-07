# SKHU Connect ERD

> Last Updated: 2026-08-06
>
> 본 문서는 SKHU Connect 백엔드의 MVP 데이터베이스 설계 기준이다.
> Entity 구현과 데이터베이스 변경은 `ARCHITECTURE.md`, API 명세서, 본 문서를 기준으로 진행한다.
>
> 본 문서에 없는 Entity, 컬럼, 관계, 제약조건은 임의로 추가하지 않는다.
> 변경이 필요한 경우 구현 전에 문서를 먼저 수정한다.

---

# 1. 설계 원칙

- 데이터베이스는 MySQL을 사용한다.
- 모든 기본키는 `BIGINT` 자동 증가 방식을 사용한다.
- 모든 Entity는 `BaseEntity`를 상속한다.
- `BaseEntity`는 `created_at`, `updated_at`을 관리한다.
- JPA Auditing으로 생성 시각과 수정 시각을 자동 관리한다.
- 연관관계는 기본적으로 지연 로딩을 사용한다.
- 불필요한 양방향 연관관계는 만들지 않는다.
- API 응답으로 Entity를 직접 반환하지 않는다.
- Enum은 문자열로 저장한다.
- 사용자 삭제와 관리자 숨김은 서로 다른 개념으로 관리한다.
- 실제 사용자 식별 정보는 사용자 API 응답에 노출하지 않는다.
- 물리 삭제보다 논리 삭제를 우선한다.
- 중복 동의, 북마크, 댓글 공감은 데이터베이스 Unique 제약조건으로도 차단한다.

---

# 2. 전체 Entity 목록

현재 코드에 `@Entity`로 구현된 Entity는 다음과 같다.

```text
Department
User
EmailVerification
RefreshToken
Petition
Agreement
Bookmark
Comment
PetitionAnonymousNumber
CommentLike
Notification
ThresholdSetting
```

다음 항목은 후속 설계이며 현재 코드에 Entity가 없다.

```text
Admin
OfficialAnswer
NotificationLog
OfficialAnswerHistory
ThresholdSettingHistory
```

따라서 관리자·공식 답변·운영 알림 로그와 각 변경 이력은 현재 DB 스키마가 아니라 후속 구현 범위다.

---

# 3. 전체 관계

```text
Department 1 ─── N User

User 1 ─── N Petition
User 1 ─── N Agreement
User 1 ─── N Bookmark
User 1 ─── N Comment
User 1 ─── N PetitionAnonymousNumber
User 1 ─── N CommentLike
User 1 ─── N Notification
User 1 ─── 0..1 RefreshToken

Admin 1 ─── N OfficialAnswer
Admin 1 ─── N NotificationLog
Admin 1 ─── N Petition Visibility Action
Admin 1 ─── N Comment Visibility Action
Admin 1 ─── N ThresholdSetting Update

Petition 1 ─── N Agreement
Petition 1 ─── N Bookmark
Petition 1 ─── N Comment
Petition 1 ─── N PetitionAnonymousNumber
Petition 1 ─── 0..1 OfficialAnswer
Petition 1 ─── N Notification

PetitionAnonymousNumber 1 ─── N Comment

Comment 1 ─── N CommentLike
```

`Petition Visibility Action`과 `Comment Visibility Action`은 별도 Entity가 아니다.

청원과 댓글의 숨김 처리 컬럼에서 마지막 처리 관리자와 처리 정보를 저장한다.

---

# 4. 공통 BaseEntity

## BaseEntity

| 컬럼 | 타입 | Null | 설명 |
|---|---|---:|---|
| `created_at` | `DATETIME(6)` | 불가 | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 | 마지막 수정 시각 |

## 구현 규칙

- `@MappedSuperclass`를 사용한다.
- JPA Auditing을 사용한다.
- 기본키 `id`는 각 Entity에서 선언한다.
- `created_at`은 생성 후 변경하지 않는다.
- `updated_at`은 변경 시 자동 갱신한다.

---

# 5. Department

성공회대학교 학과 정보를 관리한다.

회원가입 시 사용자가 자신의 학과를 선택한다.

## 테이블명

```text
departments
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 학과 식별자 |
| `code` | `VARCHAR(50)` | 불가 | UNIQUE | 학과 코드 |
| `name` | `VARCHAR(100)` | 불가 | UNIQUE | 학과 이름 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
Department 1 : N User
```

## 제약조건

```text
UNIQUE(code)
UNIQUE(name)
```

학과 목록과 실제 코드값은 별도 데이터 작업에서 확정한다.

---

# 6. User

성공회대학교 이메일 인증을 완료한 일반 사용자를 관리한다.

## 테이블명

```text
users
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 사용자 식별자 |
| `email` | `VARCHAR(255)` | 불가 | UNIQUE | 인증된 학교 이메일 |
| `login_id` | `VARCHAR(50)` | 불가 | UNIQUE | SKHU Connect 로그인 아이디 |
| `password` | `VARCHAR(255)` | 불가 |  | BCrypt 암호화 비밀번호 |
| `department_id` | `BIGINT` | 불가 | FK | 사용자가 선택한 학과 |
| `notification_enabled` | `BOOLEAN` | 불가 | DEFAULT TRUE | 전체 웹 알림 활성화 여부 |
| `created_at` | `DATETIME(6)` | 불가 |  | 가입 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
Department 1 : N User
User 1 : N Petition
User 1 : N Agreement
User 1 : N Bookmark
User 1 : N Comment
User 1 : N CommentLike
User 1 : N Notification
User 1 : 0..1 RefreshToken
```

## 제약조건

```text
UNIQUE(email)
UNIQUE(login_id)
FOREIGN KEY(department_id) REFERENCES departments(id)
```

## 비즈니스 규칙

- 이메일은 `@office.skhu.ac.kr` 도메인만 허용한다.
- 동일 이메일로 여러 계정을 만들 수 없다.
- 동일 로그인 아이디를 중복 사용할 수 없다.
- 학교 계정 비밀번호는 저장하지 않는다.
- 비밀번호는 BCrypt 해시만 저장한다.
- 사용자 실제 ID와 이메일은 일반 사용자 API에 반환하지 않는다.

---

# 6.1 EmailVerification

성공회대 공식 이메일의 인증번호와 인증 완료 상태를 MySQL에 저장한다.

## 테이블명

```text
email_verifications
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| id | BIGINT | 불가 | PK, AUTO_INCREMENT | 이메일 인증 식별자 |
| email | VARCHAR(255) | 불가 | 복합 UNIQUE | 정규화된 성공회대 공식 이메일 |
| purpose | VARCHAR(30) | 불가 | 복합 UNIQUE | SIGN_UP 또는 PASSWORD_RESET |
| code_hash | VARCHAR(64) | 불가 |  | salt를 포함해 계산한 SHA-256 해시 |
| code_salt | VARCHAR(64) | 불가 |  | 인증 레코드별 random salt |
| code_expires_at | DATETIME(6) | 불가 |  | 인증번호 만료 시각 |
| attempt_count | INT | 불가 |  | 인증번호 입력 실패 횟수 |
| sent_at | DATETIME(6) | 불가 |  | 인증번호 발송 시각 |
| verified_at | DATETIME(6) | 가능 |  | 인증번호 검증 성공 시각 |
| token_hash | VARCHAR(64) | 가능 | UNIQUE | verificationToken SHA-256 해시 |
| token_expires_at | DATETIME(6) | 가능 |  | 인증 완료 token 만료 시각 |
| used_at | DATETIME(6) | 가능 |  | 인증 완료 token 소비 시각 |
| created_at | DATETIME(6) | 불가 |  | 생성 시각 |
| updated_at | DATETIME(6) | 불가 |  | 수정 시각 |

## 관계

User 또는 다른 Entity와의 FK 관계는 없다.

## 제약조건 및 인덱스

```text
UNIQUE INDEX ux_email_verifications_email_purpose (email, purpose)
UNIQUE INDEX ux_email_verifications_token_hash (token_hash)
```

## 비즈니스 규칙

- 이메일은 trim 후 소문자로 정규화하며 정확히 @office.skhu.ac.kr로 끝나야 한다.
- 이메일과 목적별 하나의 인증 레코드를 유지하고 재전송 시 같은 레코드를 갱신한다.
- 인증번호는 숫자 6자리이고 발송 시점부터 5분간 유효하다.
- 동일 이메일과 목적의 재전송은 발송 후 60초 동안 제한한다.
- 인증번호 입력은 최대 5회 실패할 수 있으며 5회 실패하면 사용할 수 없다.
- 재전송 시 기존 인증번호와 인증 완료 token을 폐기한다.
- 인증번호 원문은 저장하지 않고 record별 salt를 포함한 SHA-256 해시만 저장한다.
- 인증 성공 시 30분간 유효한 일회용 verificationToken을 발급한다.
- verificationToken 원문은 저장하지 않고 SHA-256 해시만 저장한다.
- 목적은 SIGN_UP과 PASSWORD_RESET으로 구분하며 다른 목적에 재사용할 수 없다.
- 사용된 token과 만료된 인증번호 또는 token은 재사용할 수 없다.
- 이메일 인증 저장에 Redis를 사용하지 않는다.

---

# 7. Admin

관리자 웹에 로그인하는 관리자 계정을 관리한다.

일반 사용자 계정과 분리한다.

## 테이블명

```text
admins
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 관리자 식별자 |
| `login_id` | `VARCHAR(50)` | 불가 | UNIQUE | 관리자 로그인 아이디 |
| `password` | `VARCHAR(255)` | 불가 |  | BCrypt 암호화 비밀번호 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
Admin 1 : N OfficialAnswer
Admin 1 : N NotificationLog
Admin 1 : N ThresholdSetting Update
```

## 제약조건

```text
UNIQUE(login_id)
```

## 비즈니스 규칙

- 관리자 공개 회원가입은 제공하지 않는다.
- 모든 관리자는 동일한 `ADMIN` 권한을 가진다.
- 관리자별 권한 차이와 카테고리 배정은 MVP에서 제외한다.
- 관리자 비밀번호는 BCrypt 해시만 저장한다.

---

# 8. RefreshToken

사용자의 Refresh Token을 관리한다.

Access Token은 데이터베이스에 저장하지 않는다.

## 테이블명

```text
refresh_tokens
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | Refresh Token 식별자 |
| `user_id` | `BIGINT` | 불가 | FK, UNIQUE | 사용자 식별자 |
| `token_hash` | `VARCHAR(64)` | 불가 | UNIQUE | Refresh Token 원문의 SHA-256 해시 |
| `expires_at` | `DATETIME(6)` | 불가 |  | 만료 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 발급 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 재발급·갱신 시각 |

## 관계

```text
User 1 : 0..1 RefreshToken
```

## 제약조건

```text
UNIQUE INDEX ux_refresh_tokens_user_id (user_id)
UNIQUE INDEX ux_refresh_tokens_token_hash (token_hash)
FOREIGN KEY(user_id) REFERENCES users(id)
```

## 비즈니스 규칙

- Access Token의 유효기간은 30분이다.
- Refresh Token의 유효기간은 14일이다.
- Refresh Token은 `SecureRandom`으로 생성한 256비트 opaque token이다.
- Refresh Token 원문은 저장하지 않고 SHA-256 `token_hash`만 저장한다.
- Refresh Token은 이름 `refreshToken`, `HttpOnly`, `Path=/connect/auth`, `SameSite=Lax`, `Max-Age=1209600`인 Cookie로 전달한다.
- Cookie의 `Secure` 여부는 `JWT_COOKIE_SECURE` 환경변수로 설정한다.
- 로그인 시 기존 활성 Refresh Token 행의 해시와 만료 시각을 교체한다.
- 재발급 시 기존 토큰을 새 토큰으로 회전한다.
- 로그아웃 시 해당 사용자의 Refresh Token을 삭제한다.
- 사용자 한 명당 활성 Refresh Token 하나만 저장한다.
- 활성 행에서 조회되지 않는 토큰은 `TOKEN_INVALID`로 처리한다.
- 활성 행에서 만료가 확인된 토큰은 `TOKEN_EXPIRED`로 처리한다.
- Redis는 MVP에서 사용하지 않는다.

관리자 Refresh Token 저장 방식은 현재 확정되지 않았으므로 본 Entity에는 사용자 Token만 포함한다.

---

# 9. Petition

사용자가 작성한 청원을 관리한다.

## 테이블명

```text
petitions
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 청원 식별자 |
| `writer_id` | `BIGINT` | 불가 | FK | 청원 작성자 |
| `category` | `VARCHAR(30)` | 불가 |  | 청원 카테고리 |
| `status` | `VARCHAR(30)` | 불가 |  | 청원 상태 |
| `title` | `VARCHAR(100)` | 불가 |  | 청원 제목 |
| `content` | `TEXT` | 불가 |  | 청원 본문 |
| `agreement_count` | `INT` | 불가 | DEFAULT 0 | 현재 동의 수 |
| `target_agreement_count` | `INT` | 불가 |  | 생성 당시 목표 동의 수 |
| `agreement_deadline` | `DATETIME(6)` | 불가 |  | 동의 마감 시각 |
| `review_started_at` | `DATETIME(6)` | 가능 |  | 임계치 도달 시각 |
| `hidden` | `BOOLEAN` | 불가 | DEFAULT FALSE | 관리자 숨김 여부 |
| `hidden_reason` | `VARCHAR(500)` | 가능 |  | 숨김 사유 |
| `hidden_at` | `DATETIME(6)` | 가능 |  | 숨김 또는 숨김 해제 처리 시각 |
| `hidden_by_admin_id` | `BIGINT` | 가능 | FK | 마지막 숨김 처리 관리자 |
| `deleted` | `BOOLEAN` | 불가 | DEFAULT FALSE | 작성자 삭제 여부 |
| `deleted_at` | `DATETIME(6)` | 가능 |  | 작성자 삭제 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 작성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N Petition
Petition 1 : N Agreement
Petition 1 : N Bookmark
Petition 1 : N Comment
Petition 1 : 0..1 OfficialAnswer
```

## 제약조건

```text
FOREIGN KEY(writer_id) REFERENCES users(id)
FOREIGN KEY(hidden_by_admin_id) REFERENCES admins(id)
```

## Enum

### PetitionCategory

```text
SCHOLARSHIP
FACILITY
DORMITORY
LIBRARY
DEPARTMENT
```

### PetitionStatus

```text
OPEN
UNDER_REVIEW
ANSWERED
EXPIRED
```

## 상태 흐름

```text
OPEN
├─ 30일 안에 임계치 달성 → UNDER_REVIEW
└─ 30일 동안 임계치 미달 → EXPIRED

UNDER_REVIEW
└─ 공식 답변 등록 → ANSWERED
```

## 비즈니스 규칙

- 청원 작성 시 상태는 `OPEN`이다.
- 청원 동의 기간은 작성 시점부터 30일이다.
- 생성 당시 계산된 임계치를 `target_agreement_count`에 저장한다.
- 이후 임계치 설정이 변경되어도 기존 청원의 목표 동의 수는 변경하지 않는다.
- 임계치 달성 시 `review_started_at`을 저장한다.
- 검토 기한은 `review_started_at`으로부터 14일이다.
- 검토 기한 초과 여부는 조회 시 계산한다.
- 검토 기한이 초과되어도 상태는 `UNDER_REVIEW`로 유지한다.
- 작성자는 자신의 청원을 수정·삭제할 수 있다.
- 청원 수정·삭제는 `OPEN` 상태이면서 동의 수가 0명일 때만 가능하다.
- 사용자 삭제는 `deleted`, 관리자 숨김은 `hidden`으로 구분한다.
- 숨김 또는 삭제된 청원은 일반 목록과 검색 결과에서 제외한다.
- 숨김 청원 직접 조회 시 숨김 안내 응답을 제공한다.
- 숨김 청원에는 동의, 동의 취소, 댓글 작성, 북마크를 허용하지 않는다.
- 만료 청원은 읽기 전용으로 보관한다.
- 만료 청원에는 동의, 동의 취소, 댓글 작성을 허용하지 않는다.
- 특정 학부를 대상으로 하는 별도 컬럼은 MVP에서 사용하지 않는다.

---

# 10. Agreement

청원 동의를 관리한다.

화면에서는 공감으로 표시하고 백엔드에서는 Agreement를 사용한다.

## 테이블명

```text
agreements
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 동의 식별자 |
| `petition_id` | `BIGINT` | 불가 | FK | 청원 식별자 |
| `user_id` | `BIGINT` | 불가 | FK | 동의 사용자 |
| `created_at` | `DATETIME(6)` | 불가 |  | 동의 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N Agreement
Petition 1 : N Agreement
```

## 제약조건

```text
UNIQUE(petition_id, user_id)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(user_id) REFERENCES users(id)
```

## 비즈니스 규칙

- 사용자 한 명은 청원 하나에 한 번만 동의할 수 있다.
- 청원 작성자도 자신의 청원에 동의할 수 있다.
- 청원 작성 시 작성자의 동의를 자동 생성하지 않는다.
- `OPEN` 상태에서는 동의와 동의 취소가 가능하다.
- 임계치 달성 후에는 기존 동의를 취소할 수 없다.
- `UNDER_REVIEW` 상태에서도 새로운 동의는 가능하다.
- `EXPIRED` 상태에서는 동의와 동의 취소가 불가능하다.
- 숨김 또는 삭제된 청원에는 동의와 동의 취소가 불가능하다.
- Agreement 생성·삭제와 `agreement_count` 변경은 하나의 트랜잭션으로 처리한다.
- 임계치 달성 시 Petition 상태를 `UNDER_REVIEW`로 변경한다.

`ANSWERED` 상태에서 신규 동의를 허용할지는 아직 확정되지 않았다.

---

# 11. Bookmark

사용자의 청원 북마크를 관리한다.

## 테이블명

```text
bookmarks
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 북마크 식별자 |
| `petition_id` | `BIGINT` | 불가 | FK | 청원 식별자 |
| `user_id` | `BIGINT` | 불가 | FK | 북마크 사용자 |
| `created_at` | `DATETIME(6)` | 불가 |  | 북마크 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N Bookmark
Petition 1 : N Bookmark
```

## 제약조건

```text
UNIQUE(petition_id, user_id)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(user_id) REFERENCES users(id)
```

## 비즈니스 규칙

- 동일 사용자가 같은 청원을 중복 북마크할 수 없다.
- 숨김 또는 삭제된 청원에는 새 북마크를 등록할 수 없다.
- 숨김 또는 삭제된 청원은 사용자 북마크 목록에서 노출하지 않는다.

만료 청원의 북마크 허용 여부는 별도로 제한하지 않는다.

---

# 12. Comment

청원의 댓글을 관리한다.

## 테이블명

```text
comments
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 댓글 식별자 |
| `petition_id` | `BIGINT` | 불가 | FK | 청원 식별자 |
| `writer_id` | `BIGINT` | 불가 | FK | 댓글 작성자 |
| `content` | `VARCHAR(1000)` | 불가 |  | 댓글 내용 |
| `anonymous_number_id` | `BIGINT` | 불가 | FK | 청원별 익명 번호 매핑 식별자 |
| `parent_comment_id` | `BIGINT` | 가능 | FK, SELF REFERENCE | 원댓글 식별자, NULL이면 원댓글 |
| `hidden` | `BOOLEAN` | 불가 | DEFAULT FALSE | 관리자 숨김 여부 |
| `hidden_reason` | `VARCHAR(500)` | 가능 |  | 숨김 사유 |
| `hidden_at` | `DATETIME(6)` | 가능 |  | 숨김 또는 숨김 해제 처리 시각 |
| `hidden_by_admin_id` | `BIGINT` | 가능 | FK | 마지막 숨김 처리 관리자 |
| `deleted` | `BOOLEAN` | 불가 | DEFAULT FALSE | 작성자 삭제 여부 |
| `deleted_at` | `DATETIME(6)` | 가능 |  | 작성자 삭제 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 댓글 작성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 댓글 수정 시각 |

## 관계

```text
User 1 : N Comment
Petition 1 : N Comment
PetitionAnonymousNumber 1 : N Comment
Comment 1 : N CommentLike
Comment 1 : N Comment (parent_comment_id 자기참조)
```

## 제약조건

```text
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(writer_id) REFERENCES users(id)
FOREIGN KEY(anonymous_number_id) REFERENCES petition_anonymous_numbers(id)
FOREIGN KEY(parent_comment_id) REFERENCES comments(id)
FOREIGN KEY(hidden_by_admin_id) REFERENCES admins(id)
```

## 익명 처리 규칙

- 청원 작성자를 포함한 모든 댓글 작성자는 청원별 익명 번호를 부여받는다.
- 번호는 각 청원에서 1번부터 순차적으로 부여한다.
- 동일 사용자는 동일 청원에서 항상 같은 번호를 사용한다.
- 다른 청원에서는 별도 번호를 부여한다.
- 댓글을 삭제한 뒤 다시 작성해도 기존 번호를 재사용한다.
- Comment에는 익명 번호 정수값을 직접 저장하지 않는다.
- Comment는 `anonymous_number_id`로 `PetitionAnonymousNumber` 매핑을 참조한다.
- 댓글 응답에는 작성자 식별값으로 `anonymousNumber`만 반환한다.
- 사용자 ID, 이메일, 로그인 ID 등 실제 사용자 식별 정보는 반환하지 않는다.
- 기존 `익명(작성자)` 예외와 Comment의 `anonymous_number` 직접 저장 방식은 폐기한다.

## 비즈니스 규칙

- 로그인한 사용자만 댓글을 작성할 수 있다.
- 작성자는 자신의 댓글을 수정·삭제할 수 있다.
- 사용자 삭제는 `deleted`, 관리자 숨김은 `hidden`으로 구분한다.
- 숨김 댓글은 원문 대신 숨김 안내 문구를 반환한다.
- `OPEN`, `UNDER_REVIEW`, `ANSWERED` 상태에서는 새로운 댓글을 작성할 수 있다.
- `EXPIRED` 상태에서는 새로운 댓글을 작성할 수 없다.
- 숨김 또는 삭제된 청원에는 새로운 댓글을 작성할 수 없다.

삭제된 댓글의 사용자 화면 표시 방식은 아직 확정되지 않았다.

---

# 12.1 PetitionAnonymousNumber

청원별 사용자 익명 번호 매핑을 영구 관리한다.

## 테이블명

```text
petition_anonymous_numbers
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 익명 번호 매핑 식별자 |
| `petition_id` | `BIGINT` | 불가 | FK, 복합 UNIQUE | 청원 식별자 |
| `user_id` | `BIGINT` | 불가 | FK, 복합 UNIQUE | 사용자 식별자 |
| `anonymous_number` | `INT` | 불가 | 복합 UNIQUE | 해당 청원에서 표시할 익명 번호 |
| `created_at` | `DATETIME(6)` | 불가 |  | 최초 번호 발급 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
Petition 1 : N PetitionAnonymousNumber
User 1 : N PetitionAnonymousNumber
PetitionAnonymousNumber 1 : N Comment
```

## 제약조건

```text
UNIQUE(petition_id, user_id)
UNIQUE(petition_id, anonymous_number)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(user_id) REFERENCES users(id)
```

- `(petition_id, user_id)`는 동일 사용자의 번호 재발급과 중복 매핑을 방지한다.
- `(petition_id, anonymous_number)`는 같은 청원 안에서 번호 중복을 방지한다.
- `anonymous_number`는 1 이상의 정수이다.
- 매핑은 댓글 삭제와 무관하게 영구 보존하며 Comment 삭제에 cascade되지 않는다.
- `Comment.petition_id`는 참조하는 `PetitionAnonymousNumber.petition_id`와 같아야 한다.
- `Comment.writer_id`는 참조하는 `PetitionAnonymousNumber.user_id`와 같아야 한다.
- Comment 생성 요청에서는 `anonymousNumberId`, `anonymousNumber`, `writerId`를 클라이언트 입력으로 받지 않는다.
- 서버는 Access Token의 인증 사용자와 경로의 `petition_id`를 기준으로 매핑을 조회하거나 발급하고, 해당 매핑만 Comment 생성에 사용한다.
- 사용자 탈퇴나 청원 삭제 시 매핑 보존·비식별화 정책은 해당 기능 구현 전에 별도로 확정한다.

## 번호 발급 및 댓글 저장 트랜잭션

새 댓글 작성은 다음 순서를 하나의 트랜잭션에서 처리한다.

```text
1. petition_id로 Petition을 PESSIMISTIC_WRITE 잠금 조회한다.
2. 청원이 댓글 작성 가능한 상태인지 검증한다.
3. (petition_id, user_id)로 기존 매핑을 조회한다.
4. 기존 매핑이 있으면 해당 anonymous_number를 재사용한다.
5. 기존 매핑이 없으면 같은 petition_id의 MAX(anonymous_number)를 조회한다.
6. 조회 결과가 없으면 1, 있으면 MAX + 1을 새 번호로 결정한다.
7. PetitionAnonymousNumber를 저장하고 즉시 flush한다.
8. 저장된 매핑을 anonymous_number_id로 참조하는 Comment를 저장한다.
9. 트랜잭션을 커밋하고 Petition 잠금을 해제한다.
```

- 같은 청원의 번호 발급 요청은 동일 Petition 행 잠금에서 직렬화된다.
- 서로 다른 청원의 요청은 서로 다른 Petition 행을 잠그므로 병렬 처리할 수 있다.
- 기존 매핑 조회도 Petition 잠금을 획득한 뒤 수행하여 조회와 신규 발급 사이 경쟁을 막는다.
- 두 Unique 제약조건은 잠금 누락이나 예외적인 경합 시 중복 저장을 차단하는 최종 안전장치이다.
- Unique 위반이 발생하면 현재 트랜잭션을 롤백한다.
- 새 트랜잭션에서 Petition을 다시 `PESSIMISTIC_WRITE`로 잠근다.
- `(petition_id, user_id)` 매핑을 1회 재조회한다.
- 매핑이 있으면 해당 번호로 댓글 생성을 1회 재시도한다.
- 매핑이 없으면 새로운 번호를 다시 발급하지 않고 동시성 충돌 오류로 종료한다.
- 반복 재시도나 무한 루프는 사용하지 않는다.

---

# 13. CommentLike

댓글 공감을 관리한다.

## 테이블명

```text
comment_likes
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 댓글 공감 식별자 |
| `comment_id` | `BIGINT` | 불가 | FK | 댓글 식별자 |
| `user_id` | `BIGINT` | 불가 | FK | 공감 사용자 |
| `created_at` | `DATETIME(6)` | 불가 |  | 공감 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N CommentLike
Comment 1 : N CommentLike
Comment 1 : N Comment (parent_comment_id 자기참조)
```

## 제약조건

```text
UNIQUE(comment_id, user_id)
FOREIGN KEY(comment_id) REFERENCES comments(id)
FOREIGN KEY(user_id) REFERENCES users(id)
```

## 비즈니스 규칙
- 사용자 한 명은 댓글 하나에 한 번만 공감할 수 있다.
- 공감한 댓글에는 공감을 취소할 수 있다.
- 숨김 또는 삭제된 댓글에는 새로운 공감을 등록할 수 없다.
- `OPEN`, `UNDER_REVIEW`, `ANSWERED`, `EXPIRED` 상태의 기존 댓글에는 공감 및 공감 취소가 가능하다.

---

# 14. Notification

사용자 웹 알림을 관리한다. `Notification` Entity와 목록·미읽음·읽음 처리는 구현되어 있으며, 공식 답변 등록 흐름이 없어 `PETITION_ANSWERED` 이벤트 호출은 아직 연결되지 않았다.

## 테이블명

```text
notifications
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 알림 식별자 |
| `receiver_id` | `BIGINT` | 불가 | FK | 알림 수신 사용자 |
| `petition_id` | `BIGINT` | 가능 | FK | 관련 청원 |
| `comment_id` | `BIGINT` | 가능 | FK | 관련 댓글 또는 대댓글 |
| `type` | `VARCHAR(50)` | 불가 |  | 사용자 알림 유형 |
| `event_key` | `VARCHAR(150)` | 불가 | UNIQUE | 동일 이벤트 중복 방지 키 |
| `is_read` | `BOOLEAN` | 불가 | DEFAULT FALSE | 읽음 여부 |
| `read_at` | `DATETIME(6)` | 가능 |  | 읽은 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N Notification
Petition 1 : N Notification
Comment 1 : N Notification
```

## 제약조건

```text
FOREIGN KEY(receiver_id) REFERENCES users(id)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(comment_id) REFERENCES comments(id)
UNIQUE(event_key)
```

## 비즈니스 규칙

- 알림 클릭 시 관련 청원 상세 페이지로 이동한다.
- 개별 알림 읽음 처리 시 해당 알림만 변경한다.
- 전체 읽음 처리 시 사용자의 읽지 않은 알림을 모두 변경한다.
- 읽은 알림도 목록에서 유지한다.
- 웹 브라우저 Push 알림은 MVP에서 제외한다.
- `notification_enabled=false`인 사용자에게는 새 알림을 생성하지 않는다.

사용자 알림 유형과 수신 대상은 본 문서의 사용자 알림 확정 정책을 따른다.

---

# 15. OfficialAnswer

관리자가 등록하는 청원 공식 답변을 관리한다.

## 테이블명

```text
official_answers
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 공식 답변 식별자 |
| `petition_id` | `BIGINT` | 불가 | FK, UNIQUE | 청원 식별자 |
| `admin_id` | `BIGINT` | 불가 | FK | 답변 등록·마지막 수정 관리자 |
| `content` | `VARCHAR(1000)` | 불가 |  | 답변 내용 |
| `answer_source` | `VARCHAR(30)` | 불가 |  | 답변 출처 |
| `created_at` | `DATETIME(6)` | 불가 |  | 답변 등록 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 마지막 수정 시각 |

## 관계

```text
Petition 1 : 0..1 OfficialAnswer
Admin 1 : N OfficialAnswer
```

## 제약조건

```text
UNIQUE(petition_id)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(admin_id) REFERENCES admins(id)
```

## Enum

### AnswerSource

```text
OPERATION_TEAM
SCHOOL_OFFICIAL
```

## 비즈니스 규칙

- 청원 하나에는 공식 답변 하나만 등록할 수 있다.
- 공식 답변은 최대 1,000자이다.
- 답변 등록과 동시에 청원 상태를 `ANSWERED`로 변경한다.
- 공식 답변은 등록 후 수정할 수 있다.
- 첨부파일과 임시 저장은 MVP에서 제외한다.
- 만료 청원에는 기본적으로 공식 답변을 등록하지 않는다.

답변 수정 이력 조회 API는 History Entity가 없어 현재 ERD만으로 구현할 수 없다.

---

# 16. ThresholdSetting

카테고리별 청원 동의 임계치를 관리한다.

## 현재 구현 범위

현재 사용자 웹에서는 청원 생성 시 목표 동의 수를 계산하기 위한 기본 임계치 도메인만 구현한다.

관리자 정보, 변경 사유, 관리자 수정 기능과 변경 이력은 후속 관리자 웹 범위로 분리한다.

## 테이블명

```text
threshold_settings
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 임계치 설정 식별자 |
| `category` | `VARCHAR(30)` | 불가 | UNIQUE | 청원 카테고리 |
| `total_student_count` | `INT` | 불가 |  | 학교 전체 기준 학생 수 |
| `threshold_rate` | `DECIMAL(5,4)` | 불가 |  | 카테고리별 임계 비율 |
| `minimum_count` | `INT` | 불가 |  | 최소 임계 인원 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 마지막 변경 시각 |

## 제약조건

```text
UNIQUE(category)
```

## 초기 비율

| 카테고리 | 비율 |
|---|---:|
| `SCHOLARSHIP` | 0.01 |
| `FACILITY` | 0.01 |
| `LIBRARY` | 0.01 |
| `DORMITORY` | 0.005 |
| `DEPARTMENT` | 0.005 |

모든 카테고리의 초기 최소 임계치는 5명이다.

위 비율과 최소 임계치는 카테고리별 기본 정책값이다.

전체 학생 수 초기값은 아직 확정되지 않았다. 따라서 현재 사용자 웹 기본 도메인 구현에서는 `threshold_settings` 기본 행을 자동 삽입하지 않는다.

ThresholdSetting 생성 시 `totalStudentCount`, `thresholdRate`, `minimumCount`를 명시적으로 제공한다.

## 계산 정책

```text
계산 임계치 = ceil(학교 전체 학생 수 × 카테고리별 비율)

최종 임계치 = max(계산 임계치, 최소 임계치)
```

## 비즈니스 규칙

- 모든 인증 학생이 모든 카테고리 청원에 동의할 수 있다.
- 학교 전체 학생 수를 모든 카테고리의 기준 인원으로 사용한다.
- 기숙사생 여부와 학부 소속 여부는 MVP에서 검증하지 않는다.
- 변경된 설정은 이후 생성되는 청원부터 적용한다.
- 기존 청원의 `target_agreement_count`는 변경하지 않는다.

## 후속 관리자 웹 범위

다음 필드와 관계는 Admin Entity 및 관리자 임계치 수정 기능 구현 시 추가한다.

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `updated_by_admin_id` | `BIGINT` | 가능 | FK | 마지막 변경 관리자 |
| `change_reason` | `VARCHAR(500)` | 가능 |  | 마지막 변경 사유 |

관계:

```text
Admin 1 : N ThresholdSetting Update
```

후속 제약조건:

```text
FOREIGN KEY(updated_by_admin_id) REFERENCES admins(id)
```

관리자 변경은 이후 생성되는 청원에만 적용하며 기존 청원의 `target_agreement_count`는 변경하지 않는다.

임계치 변경 이력 Entity와 조회 API는 별도 후속 범위이다.

---

# 17. NotificationLog

관리자 웹에서 조회하는 운영 알림 로그를 관리한다.

## 테이블명

```text
notification_logs
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 로그 식별자 |
| `type` | `VARCHAR(50)` | 불가 |  | 관리자 알림 로그 유형 |
| `admin_id` | `BIGINT` | 가능 | FK | 관련 관리자 |
| `target_type` | `VARCHAR(30)` | 가능 |  | 관련 대상 종류 |
| `target_id` | `BIGINT` | 가능 |  | 관련 대상 식별자 |
| `description` | `VARCHAR(1000)` | 불가 |  | 로그 설명 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
Admin 1 : N NotificationLog
```

`admin_id`는 자동 생성 로그를 위해 Null을 허용한다.

`target_type`, `target_id`는 범용 참조값이며 데이터베이스 FK로 연결하지 않는다.

## 제약조건

```text
FOREIGN KEY(admin_id) REFERENCES admins(id)
```

## Enum

### NotificationLogType

```text
THRESHOLD_REACHED
ANSWER_REGISTERED
ANSWER_UPDATED
REVIEW_DEADLINE_APPROACHING
REVIEW_DEADLINE_EXCEEDED
PETITION_HIDDEN
COMMENT_HIDDEN
THRESHOLD_SETTING_UPDATED
```

## 비즈니스 규칙

- 관리자 알림 로그는 조회 전용이다.
- 관리자가 수정하거나 삭제할 수 없다.
- 읽음 상태를 관리하지 않는다.
- 자동 발생 이벤트는 `admin_id` 없이 생성할 수 있다.
- 관리자 작업 이벤트는 처리한 관리자 ID를 저장한다.

`target_type`의 구체적인 Enum 값은 로그 API 구현 시 확정한다.

---

# 18. 주요 인덱스

다음 인덱스는 API 조회 조건과 중복 방지를 기준으로 사용한다.

## User

```text
UNIQUE INDEX ux_users_email (email)
UNIQUE INDEX ux_users_login_id (login_id)
INDEX ix_users_department_id (department_id)
```

## EmailVerification

```text
UNIQUE INDEX ux_email_verifications_email_purpose (email, purpose)
UNIQUE INDEX ux_email_verifications_token_hash (token_hash)
```

## Admin

```text
UNIQUE INDEX ux_admins_login_id (login_id)
```

## RefreshToken

```text
UNIQUE INDEX ux_refresh_tokens_user_id (user_id)
UNIQUE INDEX ux_refresh_tokens_token_hash (token_hash)
```

## Petition

```text
INDEX ix_petitions_status_created_at (status, created_at)
INDEX ix_petitions_category_created_at (category, created_at)
INDEX ix_petitions_hidden_deleted (hidden, deleted)
INDEX ix_petitions_writer_id_created_at (writer_id, created_at)
INDEX ix_petitions_status_agreement_deadline (status, agreement_deadline)
```

## Agreement

```text
UNIQUE INDEX ux_agreements_petition_user (petition_id, user_id)
INDEX ix_agreements_user_id_created_at (user_id, created_at)
```

## Bookmark

```text
UNIQUE INDEX ux_bookmarks_petition_user (petition_id, user_id)
INDEX ix_bookmarks_user_id_created_at (user_id, created_at)
```

## Comment

```text
INDEX ix_comments_petition_id_created_at (petition_id, created_at)
INDEX ix_comments_writer_id_created_at (writer_id, created_at)
INDEX ix_comments_anonymous_number_id (anonymous_number_id)
INDEX ix_comments_parent_comment_id_created_at (parent_comment_id, created_at)
INDEX ix_comments_hidden_deleted (hidden, deleted)
```

## PetitionAnonymousNumber

```text
UNIQUE INDEX ux_petition_anonymous_numbers_petition_user (petition_id, user_id)
UNIQUE INDEX ux_petition_anonymous_numbers_petition_number (petition_id, anonymous_number)
INDEX ix_petition_anonymous_numbers_user_id (user_id)
```

## CommentLike

```text
UNIQUE INDEX ux_comment_likes_comment_user (comment_id, user_id)
```

## Notification

```text
UNIQUE INDEX ux_notifications_event_key (event_key)
INDEX ix_notifications_receiver_read_created (receiver_id, is_read, created_at)
INDEX ix_notifications_petition_id (petition_id)
INDEX ix_notifications_comment_id (comment_id)
```

## OfficialAnswer

```text
UNIQUE INDEX ux_official_answers_petition_id (petition_id)
```

## ThresholdSetting

```text
UNIQUE INDEX ux_threshold_settings_category (category)
```

## NotificationLog

```text
INDEX ix_notification_logs_type_created_at (type, created_at)
INDEX ix_notification_logs_created_at (created_at)
```

전체 인덱스는 실제 조회 쿼리와 실행 계획을 확인한 후 조정할 수 있다.

--- 
# 19. 삭제 및 숨김 정책

## 사용자 삭제

사용자가 자신의 청원 또는 댓글을 삭제하면 다음 값을 변경한다.

```text
deleted = true
deleted_at = 현재 시각
```

물리적으로 Row를 삭제하지 않는다.

## 관리자 숨김

관리자가 청원 또는 댓글을 숨기면 다음 값을 변경한다.

```text
hidden = true
hidden_reason = 사유
hidden_at = 현재 시각
hidden_by_admin_id = 처리 관리자
```

숨김 해제 시 `hidden`을 `false`로 변경한다.

숨김 해제 후 사유와 처리 관리자 정보를 유지할지 초기화할지는 구현 전에 확정한다.

## 조회 원칙

- 일반 사용자 목록에서는 `deleted = false`, `hidden = false` 데이터만 조회한다.
- 관리자는 숨김 데이터를 조회할 수 있다.
- 사용자가 삭제한 데이터를 관리자 화면에서 조회할지는 아직 확정되지 않았다.
- 숨김 댓글은 원문 대신 안내 문구를 반환한다.
- 삭제 댓글의 표시 방식은 아직 확정되지 않았다.

---

# 20. 상태별 기능 허용 범위

| 기능 | OPEN | UNDER_REVIEW | ANSWERED | EXPIRED |
|---|---:|---:|---:|---:|
| 청원 조회 | 가능 | 가능 | 가능 | 가능 |
| 신규 동의 | 가능 | 가능 | 불가 | 불가 |
| 동의 취소 | 가능 | 불가 | 불가 | 불가 |
| 댓글 조회 | 가능 | 가능 | 가능 | 가능 |
| 댓글 작성 | 가능 | 가능 | 가능 | 불가 |
| 댓글 공감 | 가능 | 가능 | 가능 | 가능 |
| 북마크 | 가능 | 가능 | 가능 | 가능 |
| 청원 수정 | 동의 0명일 때 가능 | 불가 | 불가 | 불가 |
| 청원 삭제 | 동의 0명일 때 가능 | 불가 | 불가 | 불가 |
| 공식 답변 등록 | 불가 | 가능 | 불가 | 원칙상 불가 |
| 공식 답변 수정 | 해당 없음 | 해당 없음 | 가능 | 해당 없음 |
숨김 또는 사용자 삭제 상태에서는 상태값과 관계없이 새로운 참여 기능을 차단한다.

---

# 21. API 명세와 ERD 간 미확정·충돌 항목

다음 항목은 현재 합의된 범위만으로 확정할 수 없으므로 Codex가 임의로 결정하지 않는다.

## 21.1 이메일 인증

다음 API는 MySQL의 EmailVerification Entity를 사용한다.

```text
POST /connect/auth/email-verifications
POST /connect/auth/email-verifications/confirm
```

확정 사항:

- 이메일과 인증 목적별 하나의 레코드를 MySQL에 저장한다.
- 인증번호 원문 대신 random salt를 포함한 SHA-256 해시를 저장한다.
- 인증번호는 5분간 유효하고 재전송은 60초 동안 제한한다.
- 인증 실패는 최대 5회이며 재전송 시 기존 인증 상태를 갱신한다.
- 인증 성공 시 30분간 유효한 일회용 verificationToken을 발급한다.
- verificationToken 원문 대신 SHA-256 tokenHash를 저장한다.
- SIGN_UP과 PASSWORD_RESET 목적을 구분한다.
- Redis는 사용하지 않는다.

실제 SMTP 제공자는 아직 확정되지 않았다.

## 21.2 비밀번호 재설정

비밀번호 재설정은 기존 이메일 인증 API를 `purpose=PASSWORD_RESET`으로 재사용하는 방향으로 구현한다.

```text
POST /connect/auth/email-verifications
POST /connect/auth/email-verifications/confirm
POST /connect/auth/password/reset
```

## 21.3 Administrator Refresh Token

Administrators use a refresh-token structure separate from users.

```text
admin_refresh_tokens
```

- One active token is stored per administrator.
- `admin_id` and `token_hash` are unique; raw refresh tokens are never stored.
- Tokens expire after 14 days and rotate on refresh.
- The administrator cookie is named `adminRefreshToken` and is scoped to `/connect/admin/auth`.
- The user `refreshToken` cookie and `refresh_tokens` table are not used by administrator authentication.
## 21.4 답변 수정 이력

다음 API가 존재하지만 `OfficialAnswerHistory`는 MVP Entity에서 제외되어 있다.

```text
GET /connect/admin/petitions/{petitionId}/answer/history
```

## 21.5 임계치 변경 이력

다음 API가 존재하지만 `ThresholdSettingHistory`는 MVP Entity에서 제외되어 있다.

```text
GET /connect/admin/threshold-settings/history
```

## 21.6 익명 번호 동시성 확정

익명 번호 동시성 정책은 다음과 같이 확정하였다.

- 별도 `PetitionAnonymousNumber` Entity와 테이블을 사용한다.
- Petition 행 `PESSIMISTIC_WRITE` 잠금으로 같은 청원의 신규 번호 발급을 직렬화한다.
- `(petition_id, user_id)`와 `(petition_id, anonymous_number)` Unique 제약조건을 적용한다.
- Comment는 번호를 직접 저장하지 않고 매핑 FK를 참조한다.
- 매핑은 댓글 삭제 후에도 영구 보존한다.

상세 트랜잭션 순서는 `12.1 PetitionAnonymousNumber`를 따른다.

## 21.7 Content moderation recovery policy

- Administrator management APIs list and manage only `deleted = false` petitions and comments; user deletion remains separate from administrator hiding.
- Restoring a petition, comment, or reply changes only `hidden` to `false`.
- `hidden_reason`, `hidden_by_admin_id`, and `hidden_at` preserve the latest hide processing record after restoration.
## 21.8 사용자 알림 정책

사용자 알림 유형, 수신 대상, 중복 방지, 읽음 및 조회 정책은 Notification 절과 실제 Notification 코드에 구현되어 있다. 공식 답변 알림의 호출 연결은 후속 범위다.

---

# 22. 구현 전 준수 사항

Codex는 다음 원칙을 따른다.

1. `AGENTS.md`, `ARCHITECTURE.md`, API 명세서, `ERD.md`를 먼저 읽는다.
2. 문서 간 충돌이 있으면 구현하지 말고 사용자에게 보고한다.
3. 미확정 항목을 임의로 구현하지 않는다.
4. Entity를 API 응답으로 직접 반환하지 않는다.
5. 모든 관계는 기본적으로 지연 로딩을 사용한다.
6. 중복 참여 데이터에는 Unique 제약조건을 적용한다.
7. 관리자 숨김과 사용자 삭제를 혼동하지 않는다.
8. 실제 사용자 정보는 일반 사용자 API에 반환하지 않는다.
9. 데이터베이스 변경 사항을 작업 완료 보고서에 전부 명시한다.
10. 구현 완료 후 관련 테스트와 전체 빌드를 실제로 실행한다.

## Comment 대댓글 정책

- `parent_comment_id IS NULL`은 원댓글, 값이 있으면 대댓글이다.
- 부모 댓글은 같은 `petition_id`의 원댓글이어야 하며 자기참조 깊이는 1단계로 제한한다.
- 삭제·숨김 부모에는 새 대댓글을 생성하지 않는다.
- 원댓글 삭제 시 대댓글은 cascade 삭제하지 않는다. 활성 대댓글이 존재하면 삭제 원댓글을 안내 문구와 함께 유지하고, 없으면 조회에서 제외한다.
- 원댓글 페이지 조회 후 `parent_comment_id IN (...)`으로 대댓글을 일괄 조회하며 부모별로 그룹핑한다.
- 원댓글과 대댓글은 각각 `created_at ASC, id ASC`로 정렬한다.
- 원댓글 응답은 `replies` 배열을 포함하고, 대댓글 응답은 `parentCommentId`를 포함하며 중첩 `replies`는 두지 않는다.
- 대댓글은 원댓글과 같은 `PetitionAnonymousNumber` 체계를 사용한다. 기존 매핑은 재사용하고 최초 활동 사용자는 기존 발급 정책으로 새 매핑을 발급한다.
## 사용자 알림 확정 정책

- 유형은 `PETITION_AGREEMENT_60_PERCENT`, `PETITION_AGREEMENT_100_PERCENT`, `PETITION_UNDER_REVIEW`, `PETITION_ANSWERED`, `COMMENT_REPLY`, `COMMENT_LIKE`, `REPLY_LIKE`이다.
- 청원 작성자는 60%, 100%, 검토 시작, 공식 답변 알림을 받는다.
- 청원 동의자는 검토 시작과 공식 답변 알림을 받되 작성자는 중복 수신하지 않는다.
- 원댓글 작성자는 대댓글과 원댓글 공감 알림을, 대댓글 작성자는 대댓글 공감 알림을 받는다.
- 자기 이벤트와 `notification_enabled=false` 수신자에게는 생성하지 않는다.
- `event_key`로 이벤트별·수신자별 최초 1회 생성을 보장한다.
- 알림은 삭제하지 않으며 개별·전체 읽음, 최신순 목록, 읽지 않은 개수 조회를 지원한다.