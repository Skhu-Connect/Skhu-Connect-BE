# SKHU Connect 개인정보 처리 정책

이 문서는 현재 백엔드 코드의 실제 처리 동작과 확정된 운영 정책을 정리한다. 공개 웹 개인정보처리방침의 법률 문안은 운영 주체와 외부 서비스의 계약·리전을 확인한 뒤 별도로 확정한다.

## 1. 회원가입

- 회원가입에는 성공회대학교 학교 이메일 인증, 로그인 아이디, 비밀번호 및 학과가 필요하다.
- 학과는 필수 항목으로 유지한다.
- 이용약관 동의는 필수로 제공한다. 회원가입 요청에서 동의 여부와 현재 버전 `1.0`을 검증한다.
- 회원가입 성공 시 별도 `UserTermsAgreement`에 사용자, 약관 버전, 서버 기준 동의 시각을 저장한다. 약관 본문과 개인정보처리방침 동의 이력은 저장하지 않는다.
- 서비스 제공에 필요한 개인정보를 일괄적인 `필수 수집·이용 동의` 체크박스로 묶지 않는다. 회원가입 화면에서 처리 항목, 목적, 보유 정책을 안내하고 개인정보처리방침 링크를 제공한다.
- 비밀번호는 BCrypt로 해시하여 저장하며 학교 계정 비밀번호는 수집하지 않는다.

## 2. 푸시 알림

- 푸시 알림은 회원가입과 분리된 선택 기능이다.
- iOS 알림 권한을 허용한 사용자에 한해 FCM Token을 별도 API로 등록한다.
- 푸시를 선택하지 않아도 회원가입과 기본 서비스 이용이 가능해야 한다.
- 현재 알림은 서비스 알림이며 광고성 알림과 구분한다. 현재 광고성 정보 수신 동의는 추가하지 않는다.
- 등록된 FCM Token은 회원 탈퇴 시 삭제한다.

## 3. 회원 탈퇴 시 실제 처리

- `User`는 `deleted=true`와 `deletedAt`을 기록하는 soft delete 방식으로 유지한다.
- `email`, `loginId`, `password`는 기존 userId를 이용한 비식별 내부 값으로 변경한다.
- Refresh Token과 FCM Token은 탈퇴 트랜잭션에서 삭제한다.
- 공개 청원·댓글·답글은 유지하고 기존 탈퇴 User FK를 계속 참조한다. 재가입 계정으로 소유권을 이전하지 않는다.
- 북마크와 사용자 Notification은 현재 탈퇴 시 즉시 삭제하지 않는다.
- 정규화된 학교 이메일의 SHA-256 해시와 탈퇴 시각을 `UserWithdrawalHistory`에 저장해 30일 재가입 제한을 확인한다. 현재 이 이력은 30일 후 자동 삭제되지 않는다.
- 따라서 공개 개인정보처리방침에 북마크·사용자 Notification의 즉시 삭제 또는 탈퇴 이력의 30일 후 자동 파기를 실제 구현 전까지 기재하지 않는다.

## 4. App Store 개인정보 분류

현재 확정된 App Store Connect 신고 방향은 다음과 같다.

| 데이터 유형 | 사용자 연결 | 추적 | 목적 |
|---|---|---|---|
| Email Address | Yes | No | App Functionality |
| User ID | Yes | No | App Functionality |
| Other User Content | Yes | No | App Functionality |
| Device ID | Yes | No | App Functionality |
| Product Interaction | Yes | No | App Functionality |

- Email Address: 학교 이메일
- User ID: 로그인 아이디 및 내부 사용자 식별자
- Other User Content: 청원, 댓글, 답글 및 신고 내용
- Device ID: FCM/APNs 푸시와 관련된 기기 토큰
- Product Interaction: 청원 동의, 댓글·답글 좋아요, 북마크 및 알림 읽음 상태
- 현재 광고 목적 처리와 타사 추적은 확인되지 않았다.

## 5. 외부 처리

- Firebase Messaging: FCM Token과 서비스 알림 전송
- Resend: 학교 이메일 인증 메일 전송
- Railway/DB: 서비스와 데이터베이스 운영

수탁자 법인명, 처리 국가·리전, 보유기간 및 국외 이전 세부사항은 운영 계약을 확인한 뒤 공개 개인정보처리방침에 반영한다.

## 6. 프론트/iOS TODO

- [ ] 회원가입 화면에 필수 이용약관 동의 UI 제공
- [ ] 개인정보 처리 항목·목적·보유 정책 안내 및 개인정보처리방침 링크 제공
- [ ] 회원가입과 분리된 푸시 알림 선택 화면 제공
- [ ] iOS 알림 권한 허용 후에만 FCM Token 등록
- [ ] 알림 권한 철회 또는 푸시 해제 시 FCM Token 삭제 요청
- [ ] 공개 `privacy-policy.html` 작성·배포 및 앱/웹에서 접근 가능한 URL 연결

현재 백엔드 저장소에는 회원가입 약관·개인정보 안내 UI와 공개 `privacy-policy.html`이 없다. 개인정보처리방침 동의 이력 엔티티는 추가하지 않는다.
