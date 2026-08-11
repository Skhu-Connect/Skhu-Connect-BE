# AGENTS.md

## 1. 프로젝트 정체성

이 저장소는 SKHU Connect 백엔드(`org.skhuconnect`)다. 다른 프로젝트의 요구사항·코드·관례를 섞지 않는다. 실제 저장소와 테스트를 최우선 사실로 사용한다.

## 2. 작업 시작 절차

모든 작업 전에 다음을 확인한다.

1. `git branch --show-current`
2. `git status --short`
3. `git log dev --oneline -5`
4. `README.md`, `AGENTS.md`, `ARCHITECTURE.md`, `ERD.md`
5. `build.gradle`, `application.yml`
6. 관련 Entity·Repository·Service·Controller·DTO·예외·테스트

문서와 코드가 충돌하면 임의로 선택하지 말고 충돌을 보고한다. 존재 여부를 확인하지 않은 클래스, API, 정책을 가정하지 않는다.

## 3. 현재 기준 상태 (2026-08-11)

로컬 `main` 기준 커밋은 `670d084`, 로컬 `dev` 기준 커밋은 `903716c`이며 두 브랜치의 코드 트리는 같다. 현재 작업 브랜치는 `main`이고 이번 문서 정리 시작 시 작업 트리는 깨끗하다.

완료 기능은 공통 기반, 학과, Swagger, 사용자·인증, 비밀번호 재설정, 회원 탈퇴·30일 재가입 제한, 임계치, 청원 CRUD·조회·동의·10분 작성 쿨다운·공유 공개 조회, 북마크, 댓글·공감·익명 번호·대댓글, 알림, 관리자 인증·콘텐츠 관리·공식 답변·운영 로그, 사용자 정보·활동 조회다.

현재 진행 중인 기능 구현은 없으며, 확인된 문서와 한글 문자열을 실제 코드 상태에 맞게 정리 중이다.

## 4. 기술 환경

- Java 17, Spring Boot 4.1.0, Gradle Groovy
- Spring Web MVC, Validation, Data JPA, Mail
- Spring Security Crypto, OAuth2 JOSE
- MySQL, `spring.jpa.hibernate.ddl-auto=update`, Open Session in View 비활성화
- Springdoc Swagger UI
- 환경변수 기반 설정이며 비밀값을 코드·문서·로그에 기록하지 않는다.

## 5. Git 전략

- `main`: 안정 브랜치. 직접 개발하지 않는다.
- `dev`: 완료 기능 통합 브랜치.
- `feat/{issue}-{name}`, `docs/{issue}-{name}`, `fix/{issue}-{name}`: 최신 로컬 `dev`에서 생성한다.
- 사용자 요청 없이는 branch 생성, commit, push, PR, merge, rebase, reset을 하지 않는다.
- force push와 `git reset --hard`를 사용하지 않는다.
- 기존 미커밋 변경은 사용자 작업으로 보고 보존한다.
- 커밋 메시지는 `<type>: <한국어 요약>` 형식으로 작성한다.

## 6. 구현 순서

1. 문서·코드·테스트 분석
2. 정책 충돌과 선행 조건 확인
3. 필요한 경우 `ARCHITECTURE.md`, `ERD.md` 먼저 갱신
4. Entity와 DB 제약
5. Repository
6. Service와 트랜잭션
7. DTO·Controller·Swagger·예외
8. Entity·Service·MVC·JPA·동시성 테스트
9. `git diff --check`, `clean test`, `clean build`
10. 변경 파일·DB·API·검증·남은 항목 보고

## 7. 코딩 규칙

- 기존 패키지와 스타일을 따른다.
- Controller는 HTTP 처리만, 비즈니스 로직과 트랜잭션은 Service에 둔다.
- Repository를 Controller에서 직접 사용하지 않는다.
- JPA Entity를 API 응답으로 반환하지 않는다.
- 연관관계는 기본 LAZY, 생명주기가 다른 Entity에 무분별한 cascade를 쓰지 않는다.
- DB 중복 방지는 사전 조회뿐 아니라 UNIQUE 제약으로 보장한다.
- 목록 정렬에는 같은 시간의 안정성을 위한 ID 보조 정렬을 둔다.
- 인증 사용자 ID를 요청 DTO로 받지 않고 Access Token에서 얻는다.
- userId, loginId, email 등 식별정보를 익명 댓글 응답에 노출하지 않는다.
- 불필요한 인터페이스·추상화·프레임워크·의존성을 추가하지 않는다.
- 기존 API를 변경할 때 하위 호환과 영향 범위를 보고한다.

## 8. 인증 정책 요약

- 학교 이메일은 `@office.skhu.ac.kr`만 허용한다.
- 비밀번호는 BCrypt로 저장한다.
- Access Token은 HS256 JWT, `sub=User ID`, `role=USER`, 유효기간 30분이다.
- Refresh Token은 256-bit opaque token이며 DB에는 SHA-256 해시만 저장하고 14일간 유효하다.
- Refresh Token은 HttpOnly Cookie로 전달하고 로그인·재발급 시 교체한다.
- 공개 GET은 청원 목록·상세와 댓글 목록뿐이다. 댓글 GET은 토큰이 없으면 익명, 토큰이 있으면 반드시 검증한다.
- 변경 API와 알림 API는 인증 필수다.

## 9. 핵심 도메인 정책

자세한 내용은 `ARCHITECTURE.md`, 스키마는 `ERD.md`가 기준이다.

- 청원: `OPEN → UNDER_REVIEW → ANSWERED`, 기한 미달성 시 `EXPIRED`; hidden/deleted는 사용자 기능에서 제외.
- 북마크: 사용자·청원 1회 UNIQUE, 중복 409, 미등록 취소 404, 최신순+ID 정렬.
- 댓글: OPEN/UNDER_REVIEW/ANSWERED 작성 가능, EXPIRED 작성 불가. ANSWERED/EXPIRED 기존 댓글 공감 가능.
- 익명 번호: 청원별 1부터 순차, 사용자·청원별 영구 재사용, 별도 매핑 Entity, Petition 행 비관적 잠금.
- 대댓글: 1단계, 같은 청원의 활성·비숨김 원댓글만 부모 가능. 원댓글 삭제 후 대댓글 유지.
- 알림: 정책은 확정됐지만 현재 브랜치 구현 검증 중이다.

## 10. DB·보안 안전

- 스키마·컬럼·FK·UNIQUE·인덱스 변경은 영향과 기존 데이터 위험을 보고한다.
- 운영·Railway 데이터를 임의 변경하지 않는다.
- 비밀값, 토큰, 개인정보를 출력·커밋하지 않는다.
- Spring Security/JWT 정책을 테스트 편의를 위해 약화하지 않는다.
- 파괴적 파일·Git·DB 명령은 명시적 승인 없이는 실행하지 않는다.

## 11. 테스트

변경 위험에 맞춰 단위, MVC, JPA 통합, 동시성 테스트를 추가한다. 최종적으로 가능한 경우 다음을 실행한다.

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean build
git diff --check
git diff --name-only
git status --short
```

DB 환경변수 미설정으로 실패한 테스트와 코드 결함을 구분한다. 실행하지 않은 검증을 성공했다고 말하지 않는다.

## 12. 완료 보고

최종 보고에는 요청 요약, 시작 브랜치·상태, 생성/수정/삭제 파일, 파일별 핵심 변경, 설계 결정, 의존성, DB 영향, API, 실제 실행한 검증과 결과, 미검증·미확정 사항, 최종 `git status`를 포함한다. commit·push·PR·merge 수행 여부를 명시한다.