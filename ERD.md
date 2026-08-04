# SKHU Connect ERD

> Last Updated: 2026-08-04
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

MVP Entity는 다음과 같다.

```text
Department
User
Admin
RefreshToken
Petition
Agreement
Bookmark
Comment
CommentLike
Notification
OfficialAnswer
ThresholdSetting
NotificationLog
```

다음 Entity는 현재 MVP ERD에서 제외한다.

```text
OfficialAnswerHistory
ThresholdSettingHistory
```

답변 수정 이력 API와 임계치 변경 이력 API를 MVP에서 구현하려면 별도 History Entity 설계가 필요하다.

---

# 3. 전체 관계

```text
Department 1 ─── N User

User 1 ─── N Petition
User 1 ─── N Agreement
User 1 ─── N Bookmark
User 1 ─── N Comment
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
Petition 1 ─── 0..1 OfficialAnswer
Petition 1 ─── N Notification

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
| `token` | `VARCHAR(500)` | 불가 | UNIQUE | Refresh Token 또는 토큰 식별값 |
| `expires_at` | `DATETIME(6)` | 불가 |  | 만료 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 발급 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 재발급·갱신 시각 |

## 관계

```text
User 1 : 0..1 RefreshToken
```

## 제약조건

```text
UNIQUE(user_id)
UNIQUE(token)
FOREIGN KEY(user_id) REFERENCES users(id)
```

## 비즈니스 규칙

- Access Token의 유효기간은 30분이다.
- Refresh Token의 유효기간은 14일이다.
- Refresh Token은 HttpOnly Cookie로 전달한다.
- 로그아웃 시 해당 사용자의 Refresh Token을 삭제한다.
- 사용자 한 명당 활성 Refresh Token 하나만 저장한다.
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
| `anonymous_number` | `INT` | 가능 |  | 청원별 익명 번호 |
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
Comment 1 : N CommentLike
```

## 제약조건

```text
FOREIGN KEY(petition_id) REFERENCES petitions(id)
FOREIGN KEY(writer_id) REFERENCES users(id)
FOREIGN KEY(hidden_by_admin_id) REFERENCES admins(id)
```

## 익명 처리 규칙

- 청원 작성자가 작성한 댓글은 `익명(작성자)`로 표시한다.
- 청원 작성자의 댓글은 `anonymous_number`를 사용하지 않는다.
- 다른 사용자는 청원별 최초 댓글 작성 순서에 따라 `익명1`, `익명2` 형태로 표시한다.
- 같은 사용자는 같은 청원에서 동일한 `anonymous_number`를 사용한다.
- 다른 청원에서는 새로운 익명 번호를 부여한다.
- 실제 사용자 ID와 이메일은 사용자 댓글 응답에 반환하지 않는다.
- 익명 번호는 Comment Service에서 기존 댓글을 조회하여 재사용한다.

## 비즈니스 규칙

- 로그인한 사용자만 댓글을 작성할 수 있다.
- 작성자는 자신의 댓글을 수정·삭제할 수 있다.
- 사용자 삭제는 `deleted`, 관리자 숨김은 `hidden`으로 구분한다.
- 숨김 댓글은 원문 대신 숨김 안내 문구를 반환한다.
- `EXPIRED` 청원에는 새로운 댓글을 작성할 수 없다.
- 숨김 또는 삭제된 청원에는 새로운 댓글을 작성할 수 없다.

`ANSWERED` 상태에서 댓글 작성을 허용할지는 아직 확정되지 않았다.

삭제된 댓글의 사용자 화면 표시 방식은 아직 확정되지 않았다.

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

만료 청원의 기존 댓글에 공감할 수 있는지는 아직 확정되지 않았다.

---

# 14. Notification

사용자 웹 알림을 관리한다.

## 테이블명

```text
notifications
```

## 컬럼

| 컬럼 | 타입 | Null | 제약조건 | 설명 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 불가 | PK, AUTO_INCREMENT | 알림 식별자 |
| `user_id` | `BIGINT` | 불가 | FK | 알림 수신 사용자 |
| `petition_id` | `BIGINT` | 가능 | FK | 관련 청원 |
| `type` | `VARCHAR(50)` | 불가 |  | 사용자 알림 유형 |
| `title` | `VARCHAR(150)` | 불가 |  | 알림 제목 |
| `content` | `VARCHAR(500)` | 불가 |  | 알림 내용 |
| `is_read` | `BOOLEAN` | 불가 | DEFAULT FALSE | 읽음 여부 |
| `read_at` | `DATETIME(6)` | 가능 |  | 읽은 시각 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 수정 시각 |

## 관계

```text
User 1 : N Notification
Petition 1 : N Notification
```

## 제약조건

```text
FOREIGN KEY(user_id) REFERENCES users(id)
FOREIGN KEY(petition_id) REFERENCES petitions(id)
```

## 비즈니스 규칙

- 알림 클릭 시 관련 청원 상세 페이지로 이동한다.
- 개별 알림 읽음 처리 시 해당 알림만 변경한다.
- 전체 읽음 처리 시 사용자의 읽지 않은 알림을 모두 변경한다.
- 읽은 알림도 목록에서 유지한다.
- 웹 브라우저 Push 알림은 MVP에서 제외한다.
- `notification_enabled`가 비활성화된 사용자에게 생성할 알림 범위는 구현 전 확인한다.

사용자 알림 유형과 수신 대상의 세부 정책은 API 구현 Issue에서 확정한다.

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
| `updated_by_admin_id` | `BIGINT` | 가능 | FK | 마지막 변경 관리자 |
| `change_reason` | `VARCHAR(500)` | 가능 |  | 마지막 변경 사유 |
| `created_at` | `DATETIME(6)` | 불가 |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | 불가 |  | 마지막 변경 시각 |

## 관계

```text
Admin 1 : N ThresholdSetting Update
```

## 제약조건

```text
UNIQUE(category)
FOREIGN KEY(updated_by_admin_id) REFERENCES admins(id)
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
- 관리자 ID, 마지막 변경 사유, 변경 시각을 현재 설정에 기록한다.

임계치 변경 이력 조회 API는 History Entity가 없어 현재 ERD만으로 구현할 수 없다.

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

## Admin

```text
UNIQUE INDEX ux_admins_login_id (login_id)
```

## Petition

```text
INDEX ix_petitions_status_created_at (status, created_at)
INDEX ix_petitions_category_created_at (category, created_at)
INDEX ix_petitions_hidden_deleted (hidden, deleted)
INDEX ix_petitions_writer_id_created_at (writer_id, created_at)
INDEX ix_petitions_agreement_deadline (status, agreement_deadline)
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
INDEX ix_comments_hidden_deleted (hidden, deleted)
```

## CommentLike

```text
UNIQUE INDEX ux_comment_likes_comment_user (comment_id, user_id)
```

## Notification

```text
INDEX ix_notifications_user_created_at (user_id, created_at)
INDEX ix_notifications_user_read (user_id, is_read)
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
| 신규 동의 | 가능 | 가능 | 미확정 | 불가 |
| 동의 취소 | 가능 | 불가 | 불가 | 불가 |
| 댓글 조회 | 가능 | 가능 | 가능 | 가능 |
| 댓글 작성 | 가능 | 가능 | 미확정 | 불가 |
| 댓글 공감 | 가능 | 가능 | 미확정 | 미확정 |
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

다음 API가 존재하지만 저장 Entity가 확정되지 않았다.

```text
POST /connect/auth/email-verifications
POST /connect/auth/email-verifications/confirm
```

미확정 사항:

- 인증번호 저장 위치
- 인증번호 유효시간
- 재전송 제한
- 인증 완료 Token 저장 방식
- 이메일 발송 서비스

## 21.2 비밀번호 재설정

다음 API가 존재하지만 재설정 Token 저장 방식이 확정되지 않았다.

```text
POST /connect/auth/password-reset/request
POST /connect/auth/password-reset/confirm
```

## 21.3 관리자 Refresh Token

`RefreshToken`은 현재 User와의 1:1 관계로만 설계되어 있다.

관리자 로그인에서도 Access Token과 Refresh Token을 모두 사용할지, 관리자 전용 Refresh Token 저장 구조를 추가할지는 확정되지 않았다.

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

## 21.6 익명 번호 동시성

현재는 Comment의 `anonymous_number`를 기존 댓글을 조회하여 재사용하는 방식이다.

같은 청원에 여러 사용자가 동시에 첫 댓글을 작성할 때 번호가 충돌하지 않도록 트랜잭션 또는 별도 익명 매핑 Entity가 필요할 수 있다.

별도 `PetitionAnonymous` Entity 도입 여부는 확정되지 않았다.

## 21.7 상태별 동작

다음 정책은 아직 확정되지 않았다.

- `ANSWERED` 청원의 신규 동의 가능 여부
- `ANSWERED` 청원의 댓글 작성 가능 여부
- `ANSWERED` 청원의 댓글 공감 가능 여부
- `EXPIRED` 청원의 기존 댓글 공감 가능 여부
- 사용자 삭제 댓글의 화면 표시 방식
- 삭제된 청원의 관리자 화면 노출 여부
- 숨김 해제 시 기존 숨김 사유 보존 여부

## 21.8 사용자 알림 정책

알림 Entity는 정의되어 있지만 다음 사항은 아직 확정되지 않았다.

- 알림 유형 전체 목록
- 유형별 수신 대상
- 동의 수 구간 알림 여부
- 알림 설정을 비활성화했을 때 생성하지 않을 알림 범위

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