# SKHU Connect 관리자 웹 정책

## 1. 관리자 인증 및 권한

- User와 Admin은 별도 계정/테이블로 관리한다.
- 관리자는 여러 명을 허용한다.
- 공개 관리자 회원가입은 제공하지 않는다.
- MVP 관리자 계정은 DB에서 직접 생성하며 비밀번호는 BCrypt 해시만 저장한다.
- 관리자 전용 login / refresh / logout API를 사용한다.
- 기존 BCrypt, JWT, Refresh Token 해싱·회전 로직은 최대한 재사용한다.
- 관리자 Access Token은 `role=ADMIN`, 유효기간 30분, Bearer 방식이다.
- ADMIN Token은 `/connect/admin/**` 관리자 보호 API에만 인증 수단으로 인정한다.
- USER Token은 관리자 API에 사용할 수 없고 ADMIN Token도 사용자 보호 API에 사용할 수 없다.
- 관리자 Refresh Token은 사용자와 DB 저장 구조·쿠키·API를 분리한다.
- 관리자 Refresh Cookie는 HttpOnly이며 운영에서는 Secure=true, SameSite=None을 사용한다.
- 로그인 실패 시 ID/비밀번호 오류를 구분하지 않는다.
- 계정 잠금, 비밀번호 찾기, 관리자별 세부 권한은 MVP에서 제외한다.

## 2. 임계치 설정

- 관리자가 `threshold_settings`를 조회·수정할 수 있다.
- 전체 학생 수, 임계치 비율, 최소 인원을 관리한다.
- 변경된 설정은 이후 생성되는 청원부터 적용한다.
- 기존 청원의 `target_agreement_count`는 변경하지 않는다.
- 변경 사유와 처리 관리자를 기록한다.
- 실시간 Push/SSE/WebSocket은 사용하지 않는다. 수정 후 다시 조회한다.

## 3. 청원·댓글 관리

- 관리자는 청원, 댓글, 대댓글을 숨김/복구할 수 있다.
- 물리 삭제하지 않는다.
- 숨김 사유, 처리 관리자, 처리 시각을 기록한다.
- 숨김 콘텐츠는 기존 사용자 노출 정책을 따른다.
- 작성자가 삭제한 콘텐츠는 숨기지 않는다. 이미 노출이 끊겨 있으므로 신고만 종결한다.
- 신고 목록은 신고 대상의 원문과 삭제·숨김 상태를 함께 반환한다. 작성자가 삭제한 뒤에도 관리자가 신고 사유를 판단할 수 있어야 하기 때문이다.
- 신고되지 않은 삭제 콘텐츠는 관리자 화면에서도 조회하지 않는다.

## 4. 공식 답변 및 상태

- 임계치 달성 시 `OPEN → UNDER_REVIEW`로 자동 전환한다.
- 관리자가 임의로 청원 상태를 변경하는 API는 제공하지 않는다.
- 공식 답변은 `UNDER_REVIEW` 청원에 등록한다.
- 공식 답변 등록 시 `UNDER_REVIEW → ANSWERED`로 자동 전환한다.
- 공식 답변은 등록 후 수정·관리자 조회할 수 있다.
- 사용자 청원 상세는 답변이 있을 때 `officialAnswer`를 포함한다.
- 공식 답변 등록 시 기존 `PETITION_ANSWERED` 사용자 알림을 연결한다.

## 5. 관리자 운영 로그

- 사용자 Notification과 관리자 운영 로그를 분리한다.
- `NotificationLog`에 운영 이벤트를 기록한다.
- 대상:
    - 임계치 달성
    - 공식 답변 등록/수정
    - 청원·댓글 숨김/복구
    - 임계치 설정 변경
- 1차에서는 조회 중심으로 구현한다.
- 관리자 실시간 Push/SSE/WebSocket은 제외한다.

## 6. 1차 제외 범위

- 신고
- 대시보드 통계
- 관리자별 세부 권한
- 관리자 계정 잠금/비밀번호 찾기

## 7. 숨김 복구 이력

- 청원, 댓글, 대댓글 복구 시 hidden만 false로 변경한다.
- 마지막 숨김의 사유, 처리 관리자, 처리 시각은 보존한다.
- 사용자 deleted 상태는 관리자 숨김/복구 대상이 아니다.

## 5.1 NotificationLog schema decisions

- target_type: PETITION, COMMENT, THRESHOLD_SETTING.
- Restore events use PETITION_RESTORED and COMMENT_RESTORED.
- Automatic threshold events store admin_id as null; administrator actions store the processing administrator.

## 8. 신고 정책 (2차)

- 인증 사용자만 청원·댓글·대댓글을 신고할 수 있다.
- 동일 사용자는 동일 대상에 한 번만 신고할 수 있다.
- 신고는 PENDING → DISMISSED 또는 ACTION_TAKEN으로 처리한다.
- 신고 유형과 상세 사유, 처리 관리자·처리 시각·처리 사유를 기록한다.
- ACTION_TAKEN 시 관리자가 조치 종류(HIDE 또는 USER_LOGIN_BAN)를 선택한다. 선택하지 않으면 400이다.
- HIDE는 기존 관리자 hidden 정책을 재사용한다. 이미 숨김이거나 작성자가 삭제한 콘텐츠는 기존 이력을 덮어쓰지 않고 신고만 종결한다.
- USER_LOGIN_BAN은 신고 대상 작성자 계정을 로그인 정지 처리한다. 대상 콘텐츠가 삭제됐어도 정지는 건다 - 글을 지운다고 제재를 피할 수 없다.
- 처리 결과는 신고자에게, ACTION_TAKEN이면 대상 작성자에게도 알림으로 통지한다(ARCHITECTURE.md 9절).
- 사용자 deleted와 관리자 hidden, 계정 login_banned는 서로 분리한다.

## 9. 공지사항·전체 알림 정책 (2차)

- 공지는 DRAFT → PUBLISHED → HIDDEN 상태로 관리한다.
- 최초 PUBLISHED 전환 시 기존 사용자 Notification/FCM 흐름으로 전체 알림을 한 번만 발송한다.
- 수정·숨김·재공개 시 알림을 재발송하지 않는다.
- notification_enabled=false 사용자는 전체 알림 수신 대상에서 제외한다.
- 공개된 공지만 사용자에게 노출하며 물리 삭제하지 않는다.
- 사용자는 공지를 삭제할 수 없고 메인 배너에서 개별 공지만 닫을 수 있다. 닫기 기록은 사용자별로만 적용된다.

## 10. 관리자 대시보드 정책 (2차)

- 기간별 집계 없이 현재 누적 수치만 제공한다.
- 제공 지표는 사용자 수, 청원 상태별 수, 동의 수, 댓글 수, 미처리 신고 수다.
- 기존 hidden/deleted 및 신고 처리 상태 기준을 재사용한다.
- 기간별·추가 통계는 범위에서 제외한다.
