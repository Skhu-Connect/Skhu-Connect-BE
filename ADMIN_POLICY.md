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
- 공지사항
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
