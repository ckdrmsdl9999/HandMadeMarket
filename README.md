# 핸드메이드스토리

수제상품 판매를 위한 핸드메이드 마켓 프로젝트입니다.

1차에서는 `Spring Boot + Thymeleaf`로 회원, 상품, 장바구니, 주문, 배송 기능을 구현했습니다.  
이후 2차에서는 프론트엔드를 `React`로 옮기고 인증 방식도 다시 정리할 예정입니다.

## 프로젝트 목표

- 회원, 상품, 장바구니, 주문, 배송 기능 구현
- 세션 기반 인증과 네이버 OAuth2 로그인 적용
- 구글 OAuth2 로그인 진입 경로 구성
- Swagger(OpenAPI) 기반 API 문서화
- AWS EC2 서버 배포 및 운영
- GitHub Actions 기반 CI/CD 적용
- Blue-Green 방식 무중단 배포 환경 구성
- HTTPS 적용 및 Nginx 기반 Reverse Proxy 설정
- 1차 MVP 완성 후 프론트엔드와 인증 구조 리팩토링

## 프로젝트 단계

### 1차 MVP

- `Spring Security + Session` 기반 인증
- `Thymeleaf` 기반 화면 구성
- 회원, 상품, 장바구니, 주문, 배송 도메인 구현
- 서버에 배포해서 실제로 동작 확인
- `Swagger(OpenAPI)` 기반 API 문서화 추가
- GitHub Actions 기반 CI/CD 적용
- AWS EC2 self-hosted runner 기반 배포 자동화
- Blue-Green 방식 무중단 배포 환경 구성
- HTTPS 적용 및 Nginx 기반 Reverse Proxy 설정

### 2차 고도화 예정

- 프론트엔드를 `React` 기반으로 리팩토링
- 인증 방식을 `JWT` 기반으로 전환
- 프론트/백 분리 구조로 정리
- 리뷰, 커뮤니티 등 확장 기능 추가

## 참여 인원 및 역할

- 참여 인원: `2명`
- 1차 MVP 담당: 백엔드와 `Thymeleaf` 기반 화면 구현
- 2차 고도화 계획: `React` 기반 프론트엔드 리팩토링과 함께 백엔드 정리, API 연동, 일부 화면 전환 작업 진행 예정

## 현재 구현 범위

### 회원/인증

- 로컬 회원가입/로그인
- 세션 기반 인증 처리
- 네이버 OAuth2 로그인
- 구글 OAuth2 로그인 진입 경로 구성
- 로그아웃 및 세션 정리
- 내 정보 조회 및 회원 정보 수정
- 회원 탈퇴
- 관리자 사용자 목록 조회
- 관리자 회원 권한 변경 및 회원 삭제

### 상품

- 상품 목록 조회
- 상품 상세 조회
- 인기 상품 조회
- 카테고리별 상품 조회
- 상품명 검색
- 판매자 본인 상품 목록 조회
- 판매자/관리자 상품 등록
- 판매자 본인 상품 수정 및 삭제
- 상품 재고 변경
- 구매 처리 시 재고 및 판매 수량 반영

### 장바구니

- 로그인 사용자 장바구니 조회
- 상품 담기
- 상품 수량 변경
- 단일 상품 삭제
- 장바구니 전체 비우기
- 홈/상품 목록/상품 상세 화면과 장바구니 API 연동

### 주문

- 로그인 사용자 기준 주문 생성
- 내 주문 목록 조회
- 주문 ID 및 주문번호 기반 주문 조회
- 주문 수령 정보 수정
- 주문 취소
- 관리자 전체 주문 조회
- 관리자 주문 상태 변경

### 배송

- 관리자 배송 목록 조회
- 배송 상세 조회
- 판매자/관리자 배송 정보 수정
- 관리자 배송 삭제
- 관리자 배송 관리 화면 구성

### 화면

- Thymeleaf 기반 메인 페이지
- 상품 검색/목록 페이지
- 상품 상세 페이지
- 장바구니 페이지
- 주문 내역 페이지
- 로그인/회원가입/내 정보 페이지
- 판매자 상품 등록 페이지
- 관리자 사용자 관리 페이지
- 관리자 배송 관리 페이지

### API 문서화

- Swagger UI 적용
- OpenAPI 기반 도메인별 API 문서 확인 가능

### CI/CD 및 배포

- GitHub Actions 기반 CI 구성
- `main`, `dev`, 기능 브랜치 및 PR 대상 빌드 검증
- `main` 브랜치 반영 시 CD 실행
- AWS EC2 self-hosted runner 기반 배포 자동화
- Gradle 빌드 후 운영 서버 배포
- Blue-Green 방식 무중단 배포 환경 구성
- HTTPS 적용
- Nginx 기반 Reverse Proxy 설정

###설계도
<img width="1068" height="662" alt="(ver1 2)핸드메이드마켓 drawio" src="https://github.com/user-attachments/assets/09f8774e-4a9d-4dd8-9e97-c77b9bfade4d" />


## 기술 스택

### Backend

- Java 17
- Spring Boot 3.4.3
- Spring Security
- Spring OAuth2 Client
- Spring Data JPA
- Thymeleaf

### Database

- PostgreSQL

### Build / Test

- Gradle
- JUnit 5
