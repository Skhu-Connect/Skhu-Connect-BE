# SKHU Connect Architecture

> Last Updated: 2026-08-27
> 기준: `fix/#57` 브랜치의 알림 종류별 설정 API 구현

## 1. 서비스와 현재 상태

SKHU Connect는 성공회대학교 학생 청원 플랫폼이다. 학생은 학교 이메일로 인증하고 청원을 등록하며, 동의·북마크·익명 댓글로 참여한다. 임계치 달성 청원은 검토와 공식 답변 단계로 이동한다.

### dev 구현 완료

- 공통 JPA·환경변수·Swagger
- Department 목록
- User, 이메일 인증, 회원가입, 로그인, JWT 재발급·로그아웃, 비밀번호 재설정
- 이메일 인증·현재 비밀번호 기반 아이디 찾기와 로그인 상태의 아이디·비밀번호 변경
- ThresholdSetting 기본 도메인
- Petition CRUD·목록·검색·상세
- Agreement 등록·취소와 상태 전환
- Bookmark
- Comment, CommentLike, PetitionAnonymousNumber, 1단계 Reply
- Notification Entity/API와 60%·100%·검토 시작·댓글 공감·대댓글 이벤트
- Notice Entity/API와 사용자별 공지 배너 닫기
- JWT `userId` 기반 내 정보와 작성 청원·동의·북마크·댓글·알림 조회
- 회원 탈퇴와 30일 재가입 제한
- 청원 10분 작성 쿨다운과 기존 공개 API 기반 청원 공유
- 관리자 인증, 임계치 관리, 콘텐츠 숨김·복구, 공식 답변 등록·수정·조회, 운영 로그와 대시보드
- 공식 답변 등록 시 `PETITION_ANSWERED` 사용자 알림 연결
- Railway의 `main` 브랜치 자동 배포

### 미구현·후속 범위

브라우저 Push 알림이 남아 있다.

## 2. 기술 구조

```text
Controller → Service(@Transactional) → Repository → MySQL
                 ↓
               DTO
```

- Java 17, Spring Boot 4.1, Gradle
- Spring Web MVC, Validation, Data JPA, Mail
- BCrypt, OAuth2 JOSE JWT
- MySQL, ddl-auto=update, open-in-view=false
- Springdoc OpenAPI
- 기본 패키지 `org.skhuconnect`
- 기능별 최상위 패키지: `auth`, `user`, `department`, `petition`, `agreement`, `bookmark`, `comment`, `threshold`, `notification`, `global`

Controller는 HTTP만 처리하고 Service가 정책·트랜잭션을 담당한다. Entity는 API에 직접 노출하지 않는다. 연관관계는 기본 LAZY다.

## 3. 인증 정책

- 학교 이메일: `@office.skhu.ac.kr`
- 이메일 인증 목적: `SIGN_UP`, `PASSWORD_RESET`, `LOGIN_ID_FIND`
- 인증번호: 숫자 6자리, 5분, 최대 5회 실패, 60초 재전송 제한
- 인증번호 원문은 저장하지 않고 salt 포함 SHA-256 해시를 저장한다.
- 인증 성공 verificationToken은 30분, 1회 사용한다.
- 비밀번호는 BCrypt다.
- Access Token: HS256, 30분, `sub=User ID`, `role=USER`
- Refresh Token: opaque 256-bit, 14일, DB에는 SHA-256 해시만 저장
- Refresh Token은 `refreshToken` HttpOnly, SameSite=Lax, Path=/connect/auth Cookie다.
- 로그인·재발급 시 Refresh Token을 회전하고 로그아웃 시 삭제한다.
- 인증 사용자 ID를 요청 본문으로 받지 않는다.
- 아이디 찾기용 인증은 가입된 이메일에만 발송하고 `LOGIN_ID_FIND` 토큰을 한 번만 소비한다.
- 로그인 상태의 아이디·비밀번호 변경은 사용자 행을 잠그고 현재 비밀번호를 재확인한다. 기존 Access Token, Refresh Token, FCM Token은 유지한다.
- 공개 API: 학과, 청원 목록·상세, 댓글 목록. 댓글 목록은 토큰이 없을 때만 익명 통과하며 잘못된 토큰은 401이다.
- 청원 변경, 동의, 북마크, 댓글 변경·공감, 알림 API는 Access Token 필수다.

## 4. 청원 정책

상태는 `OPEN`, `UNDER_REVIEW`, `ANSWERED`, `EXPIRED`다.

```text
OPEN --목표 동의 수 달성--> UNDER_REVIEW --공식 답변--> ANSWERED
OPEN --30일 내 미달성--> EXPIRED
```

- 작성자는 동의 0인 OPEN 청원만 수정·논리 삭제할 수 있다.
- 청원 등록 성공 후 10분 동안 같은 사용자의 새 청원 등록을 제한한다. 정확히 10분 후부터 허용하며, 논리 삭제된 청원도 최근 등록 시각 계산에 포함한다.
- 쿨다운 429 응답은 남은 시간을 초 단위로 올림해 `retryAfterSeconds` 속성에 포함한다.
- hidden/deleted 청원은 사용자 조회와 변경 기능에서 제외한다.
- 동의 등록은 Petition 행을 `PESSIMISTIC_WRITE`로 잠근다.
- `(petition_id,user_id)` UNIQUE로 중복 동의를 막는다.
- 작성자는 자기 청원에 동의할 수 없다(409). 이 제한을 넣기 전에 이미 생긴 자기 동의는 그대로 두며, 취소(cancel)는 이후에도 제한 없이 허용한다 - 작성자가 스스로 취소해 동의 0건으로 되돌리는 것이 유일한 탈출구다.
- 목표 달성 시 최초 한 번 `UNDER_REVIEW`와 `review_started_at`을 설정한다.
- 청원 공개 조회는 기존 동작을 유지한다.
- 청원 공유는 프론트의 기존 청원 상세 HTTPS URL을 사용하며, 백엔드는 기존 청원 상세·댓글 목록 API를 재사용한다.
- 비로그인 사용자는 공개 상태인 청원 본문과 댓글·답글을 조회할 수 있다. 동의, 댓글·답글 작성, 댓글·답글 공감 등 상태 변경은 Access Token이 필요하다.
- 사용자 삭제 또는 관리자 숨김 청원은 공유 경로에서도 조회할 수 없다. 공유 전용 API, 디바이스별 API, 자동 동의·자동 공감은 두지 않는다.

## 5. 북마크 정책

- 인증 사용자만 등록·취소·내 목록 조회 가능
- `(petition_id,user_id)` UNIQUE
- 중복 등록 409, 미등록 취소 404
- hidden/deleted 청원 등록 차단 및 목록 제외
- 최신 북마크순 `createdAt DESC, id DESC`
- 사용자 본인의 북마크만 조회

## 6. 댓글·공감 정책

- OPEN, UNDER_REVIEW, ANSWERED: 새 댓글 가능
- EXPIRED: 새 댓글 불가
- ANSWERED, EXPIRED의 기존 댓글: 공감·취소 가능
- hidden/deleted 청원: 작성·공감 불가
- 사용자 삭제 댓글은 일반 목록에서 제외하되 활성 대댓글이 있는 삭제 원댓글은 안내 문구로 유지
- 관리자 숨김 댓글은 원문 대신 `관리자에 의해 숨김 처리된 댓글입니다.` 반환
- 비로그인 목록: `myComment=false`, `liked=false`
- 작성자만 수정·논리 삭제 가능
- `(comment_id,user_id)` UNIQUE, 중복 공감 409, 미등록 취소 404
- 익명 응답에 userId, loginId, email을 노출하지 않는다.

## 7. 익명 번호 정책

- 청원 작성자를 포함한 모든 댓글 작성자에게 청원별 번호를 1부터 부여한다.
- `(petition_id,user_id)`와 `(petition_id,anonymous_number)` UNIQUE
- 동일 사용자는 동일 청원에서 원댓글·대댓글 모두 같은 번호를 영구 재사용한다.
- 다른 청원은 별도 번호다. 댓글 삭제 후 재작성해도 번호를 유지한다.
- Comment에는 정수 번호를 중복 저장하지 않고 `anonymous_number_id` FK만 둔다.
- `Comment.petition_id = mapping.petition_id`, `Comment.writer_id = mapping.user_id`를 보장한다.
- 번호 발급은 Petition `PESSIMISTIC_WRITE` 잠금 후 `MAX+1`, 매핑과 Comment를 같은 트랜잭션에 저장한다.
- UNIQUE 충돌 시 기존 트랜잭션을 롤백하고 별도 Spring Bean의 `REQUIRES_NEW`에서 Petition을 다시 잠근 뒤 매핑을 1회 재조회한다. 있으면 댓글 생성을 재시도하고 없으면 409로 종료한다. 반복 재시도하지 않는다.

## 8. 대댓글 정책

- `parent_comment_id=NULL`: 원댓글, 값 존재: 대댓글
- 깊이 1단계만 허용하고 대댓글의 대댓글은 409
- 부모는 같은 Petition의 활성·비숨김 원댓글이어야 한다.
- 원댓글이 삭제되어도 기존 대댓글은 유지한다.
- 활성 대댓글이 있는 삭제 원댓글은 `삭제된 댓글입니다.`로 반환하고, 없으면 제외한다.
- 페이지네이션은 원댓글 기준이다.
- 원댓글 `createdAt ASC,id ASC`, 각 replies도 같은 정렬이다.
- 원댓글 페이지 조회 후 부모 ID 목록으로 대댓글을 한 번에 조회해 그룹핑한다.
- 원댓글 응답은 `replies`, 대댓글은 `parentCommentId`를 포함하며 대댓글에는 replies를 중첩하지 않는다.

## 9. 사용자 알림 정책

Notification Entity, 조회·읽음 API와 주요 이벤트 연결이 `dev`에 구현되어 있다. 공식 답변 등록 흐름이 없으므로 `PETITION_ANSWERED` 호출만 미연결이다.

유형과 수신자:

- `PETITION_AGREEMENT_60_PERCENT`: 청원 작성자
- `PETITION_AGREEMENT_100_PERCENT`: 청원 작성자
- `PETITION_UNDER_REVIEW`: 작성자와 동의자, 작성자 중복 제외
- `PETITION_ANSWERED`: 작성자와 동의자, 작성자 중복 제외
- `PETITION_COMMENT_CREATED`: 청원 작성자
- `COMMENT_REPLY`: 원댓글 작성자
- `COMMENT_LIKE`: 원댓글 작성자
- `REPLY_LIKE`: 대댓글 작성자
- `REPORT_DISMISSED`: 신고자 (신고가 기각됐을 때)
- `REPORT_ACTION_TAKEN`: 신고자 (신고가 조치됐을 때, HIDE/USER_LOGIN_BAN 공통)
- `CONTENT_HIDDEN`: 신고 대상 청원·댓글 작성자 (조치 종류가 HIDE일 때만)
- `ACCOUNT_LOGIN_BANNED`: 신고 대상 작성자 (조치 종류가 USER_LOGIN_BAN일 때만)

공통 규칙:

- 동일 이벤트·수신자는 최초 1회만 생성하고 `event_key` UNIQUE로 동시 중복도 막는다.
- 청원 작성자가 아닌 사용자가 원댓글을 작성하면 청원 작성자에게 새 댓글 알림을 생성한다.
- 대댓글은 새 댓글 알림 대상에서 제외하고 `COMMENT_REPLY` 정책을 따른다.
- 자기 자신이 발생시킨 댓글·공감 알림은 생성하지 않는다.
- `notification_enabled=false`이면 새 알림을 생성하지 않는다.
- 알림 종류는 `AGREEMENT`, `ANSWER`, `REPLY`, `LIKE`, `NOTICE`, `REPORT` 포인트로 매핑하며, 사용자가 끈 포인트의 알림은 DB에 생성하지 않는다. `REPORT`는 신고 처리 결과 4종을 전부 묶는다 - 신고자용·피신고자용을 따로 끄고 켤 수 없다.
- 알림 삭제는 없다.
- 숨김·삭제된 청원을 가리키는 알림은 목록과 미읽음 개수에서 제외한다. 행 자체는 남기고 조회에서만 거른다. 청원과 무관한 `NOTICE`는 항상 노출한다. **`CONTENT_HIDDEN`·`ACCOUNT_LOGIN_BANNED`는 예외다** - 그 청원이 숨겨졌다는 사실 자체를 알리는 알림이라, 청원이 숨겨졌다고 알림까지 숨기면 안 된다(2026-08-30, 이 예외가 없어서 알림이 안 보이던 버그를 고쳤다).
- `Notification.type` 컬럼은 DB에 enum CHECK 제약을 두지 않는다 - `ddl-auto=update`가 기존 CHECK를 안 갱신해서 나중에 알림 종류를 추가할 때마다 전체 알림 발송이 500으로 끊기는 장애가 났다(ERD.md 26절 참고). 앞으로 `NotificationType`에 값을 추가할 땐 이 문제가 재발하지 않는다 - 다만 다른 enum 컬럼(`ReportStatus` 등)에 값을 추가할 땐 ERD.md 26절의 확인 절차를 따른다.
- 개별·전체 읽음은 멱등이고 `read_at`을 저장한다.
- 최신순 `createdAt DESC,id DESC`, 미읽음 개수 API 제공
- 클릭 이동을 위해 nullable `petition_id`, `comment_id` 저장
- 공식 답변 기능 구현 시 답변 저장과 `ANSWERED` 전환 트랜잭션에서 `onPetitionAnswered`를 호출해야 한다.

구현 API:

```text
GET   /connect/notifications
GET   /connect/notifications/unread-count
PATCH /connect/notifications/{notificationId}/read
PATCH /connect/notifications/read-all
```

푸시 발송:

- 알림 저장 트랜잭션에서 `FcmPushService.PushMessage` 이벤트를 발행하고 커밋 이후(`AFTER_COMMIT`) 별도 스레드에서 FCM으로 보낸다. 롤백된 알림은 발송하지 않고 FCM 왕복이 API 응답을 지연시키지 않는다.
- 이벤트는 트랜잭션 안에서 스냅샷한 값만 담는다. Open Session in View가 비활성이라 커밋 이후에는 Entity의 LAZY 연관을 참조할 수 없다.
- 수신자의 `fcm_tokens`를 조회해 토큰마다 1건씩 보낸다. `UNREGISTERED`와 `INVALID_ARGUMENT`는 만료 토큰으로 보고 삭제하며 나머지 오류는 오류 코드와 함께 기록만 한다.
- `FIREBASE_SERVICE_ACCOUNT_JSON`이 없거나 Firebase 초기화에 실패하면 기동 시 경고를 남기고 푸시만 비활성화한다. 알림 저장과 조회는 영향받지 않는다.
- 토큰 값은 기기 자격증명이므로 로그에 남기지 않고 `tokenId`만 기록한다.

## 9.1 사용자 정보·활동 조회 정책

- 모든 API는 Access Token이 필요하며 JWT `sub`에서 얻은 `userId`만 사용한다.
- `GET /connect/users/me`는 이메일, 로그인 ID, 학과 코드·이름, 알림 수신 여부를 반환하고 DB PK와 비밀번호는 반환하지 않는다.
- `GET /connect/users/me`의 `notificationSettings`는 `agreement`, `answer`, `reply`, `like`, `notice` 전체 상태를 반환한다.
- `PATCH /connect/users/me/notification-settings`는 전달된 종류별 설정만 갱신하고 갱신 후 전체 상태를 반환한다. 빈 요청은 400이다.
- `/connect/users/me/petitions`, `/agreements`, `/bookmarks`, `/comments`, `/notifications`는 본인 데이터만 조회한다.
- 청원 활동은 hidden/deleted 청원을 제외하고 기존 `PetitionQueryResponse`의 유효 상태 계산을 재사용한다.
- 댓글 활동은 삭제 댓글과 hidden/deleted 청원을 제외한다. 숨김 댓글은 기존 댓글 응답의 안내 문구 정책을 따른다.
- 기본 페이지는 `page=0,size=20`, 허용 크기는 1..100이며 `createdAt DESC,id DESC`로 정렬한다.

## 9.2 사용자 영구 차단 정책

- `POST /connect/users/me/blocks`는 청원 또는 댓글·대댓글 ID를 통해 작성자를 단방향으로 영구 차단한다. 차단 해제 API는 제공하지 않는다.
- `(blocker_id, blocked_user_id)` UNIQUE로 중복 차단을 방지하며 본인 차단은 400, 중복은 409로 처리한다.
- 로그인 사용자의 청원 목록·상세와 댓글·대댓글 목록에서만 차단 필터를 적용한다. 비로그인 공개 조회와 관리자 조회에는 적용하지 않는다.
- 작성자 ID는 응답에 노출하지 않으며, 콘텐츠 ID로 작성자를 서버에서 해석한다.
- 숨김·삭제 콘텐츠는 차단 대상으로 처리하지 않는다. 탈퇴 작성자의 기존 콘텐츠는 해당 작성자 User ID 기준으로 차단할 수 있으며, 같은 이메일로 재가입한 새 User ID에는 자동 승계되지 않는다.

## 9.3 공지사항 정책

- 공지는 관리자가 여러 개 작성할 수 있고 `DRAFT`, `PUBLISHED`, `HIDDEN` 상태로 관리한다.
- 공개 목록은 `PUBLISHED` 공지만 반환한다. 사용자는 공지를 삭제할 수 없고 메인 배너에서만 개별 공지를 닫을 수 있다.
- 사용자별 닫기 기록은 `(user_id, notice_id)`로 저장하며, 다른 사용자와 관리자 공지 원본에는 영향이 없다.
- 최초 발행 시에만 공지 알림을 발송하고, 수정·숨김·재공개·사용자 닫기에서는 재발송하지 않는다.

## 10. Git과 개발 흐름

- `main`: 안정, `dev`: 통합, 기능 브랜치는 최신 로컬 dev에서 생성
- 브랜치 예: `feat/19-user-notification`
- 요청 없이 commit/push/PR/merge 금지
- 분석 → 문서 → Entity/제약 → Repository → Service → API → 테스트 → 전체 검증 순서
- 최종 검증: `git diff --check`, `clean test`, `clean build`

## 11. 구현 원칙

- 문서와 코드가 충돌하면 중단하고 보고한다.
- 임의의 관리자/공식 답변/상태 전환 API를 만들지 않는다.
- DB 중복은 UNIQUE, 동시 상태 변경은 잠금과 트랜잭션으로 보장한다.
- 공개 API·인증·응답 계약을 임의 변경하지 않는다.
- 비밀값과 개인정보를 저장소나 로그에 노출하지 않는다.

## 12. 회원 탈퇴와 재가입 정책

- 회원 탈퇴는 `User`의 `deleted=true`, `deletedAt=현재 시각`으로 처리한다.
- 현재 비밀번호를 BCrypt로 검증한 뒤에만 사용자 식별자 비식별화, Refresh Token 및 FCM Token 삭제, 탈퇴 이력 저장을 하나의 트랜잭션에서 수행한다.
- 탈퇴 시 `email`, `loginId`, `password`는 userId를 포함한 충돌 없는 내부 값으로 변경한다. 원본 이메일은 탈퇴 이력에 저장하지 않고 정규화된 이메일의 SHA-256 해시만 저장한다.
- 탈퇴 이력의 이메일 해시와 탈퇴 시각을 기준으로 30일 동안 동일 학교 이메일 재가입을 차단한다. 30일이 지나면 기존 User를 복구하지 않고 새 User row를 생성한다.
- 청원, 댓글, 답글, 신고, 사용자 Notification, 동의, 북마크 등 기존 User FK는 변경하거나 삭제하지 않는다. 현재 구현은 탈퇴 시 북마크와 사용자 Notification을 즉시 삭제하지 않고, `UserWithdrawalHistory`도 30일 후 자동 삭제하지 않는다. 탈퇴 사용자 콘텐츠의 상태와 콘텐츠 자체의 `deleted`/`hidden` 상태는 독립적으로 유지한다.
- 기존 API에서 작성자 정보가 이미 노출되는 사용자 응답에 한해 탈퇴 작성자를 `탈퇴한 사용자`로 표시한다. 작성자 필드가 없는 청원 응답과 숫자형 `anonymousNumber`만 제공하는 댓글 응답에는 새 필드를 추가하지 않는다. 관리자 콘텐츠 응답에서는 기존 userId와 탈퇴 여부를 확인할 수 있다.
- 로그인, Refresh Token 재발급, FCM Token 등록 및 Access Token 인증 시 탈퇴 여부를 확인한다.
