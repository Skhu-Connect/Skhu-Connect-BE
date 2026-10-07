<div align="center">

<img src="https://raw.githubusercontent.com/Skhu-Connect/.github/main/profile/assets/logo.png" width="96" alt="성공잇다" />

# 성공잇다 백엔드

성공회대학교 학생들이 익명으로 건의하고, 요청을 모아 학교의 답변을 확인하는 서비스입니다.
이 저장소는 백엔드 API 기능 구현을 담당합니다.

[스웨거 주소]([https://i1000u.hueeng.com/swagger-ui/index.html)

## 주요 기능

학교 이메일로 재학생 인증을 마친 뒤 익명으로 건의를 등록합니다. 요청이 카테고리별 기준에 도달하면 검토를 거쳐 공식 답변을 확인할 수 있습니다.

| 구분 | 기능 |
| --- | --- |
| 학생 웹 | 이메일 인증, 익명 건의, 검색·필터, 요청·댓글, 북마크·공유, 유사 건의 찾기 |
| 관리자 웹 | 대시보드, 건의·신고 관리, 공식 답변, 카테고리별 도달 기준 설정, 공지사항 관리 |
| iOS 앱 | 건의 탐색·등록·요청, 댓글, 내 활동, 알림 설정, 푸시 알림 |

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 웹 | Java 17·SpringBoot·JPA·MySQL. JWT·BCrypt 인증|


## 개발 담당

| 담당 | 이름 |
| --- | --- |
| 프론트엔드 | 김석환([별도 저장소](https://github.com/Skhu-Connect/Skhu-Connect-FE)) |
| 백엔드 | 전천우  |
