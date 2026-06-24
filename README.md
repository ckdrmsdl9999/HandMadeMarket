# 핸드메이드스토리

수제상품 판매를 위한 핸드메이드 마켓 프로젝트입니다.

`Spring Boot` 기반 백엔드와 `React SPA` 기반 프론트엔드로 구성되어 있으며,  
회원, 상품, 장바구니, 주문, 배송 기능을 중심으로 핸드메이드 상품을 조회하고 구매할 수 있는 서비스를 구현했습니다.
추가 기능 구현, `Session`에서 `JWT` 기반 인증으로의 전환, 코드 리팩토링을 진행할 예정입니다.

## 프로젝트 목표

- 회원, 상품, 장바구니, 주문, 배송 기능 구현
- 세션 기반 인증과 네이버/구글 OAuth2 로그인 적용
- React SPA 기반 사용자 화면 구성
- Swagger(OpenAPI) 기반 API 문서화
- AWS EC2 서버 배포 및 운영
- GitHub Actions 기반 CI/CD 적용
- Backend / Frontend 배포 자동화 구성
- Blue-Green 방식 무중단 배포 환경 구성
- HTTPS 적용 및 Nginx 기반 Reverse Proxy 설정
- 기능 추가 및 인증 구조 리팩토링

## 프로젝트 단계

### 1차 MVP

- `Spring Security + Session` 기반 인증
- `Spring Session + Redis` 기반 세션 저장소 적용
- `React + Vite` 기반 SPA 화면 구성
- 회원, 상품, 장바구니, 주문, 배송 도메인 구현
- React Router 기반 홈, 상품, 장바구니, 주문, 로그인 페이지 라우팅 구성
- Axios 기반 API 요청 모듈 구성
- 서버에 배포해서 실제 동작 확인
- `Swagger(OpenAPI)` 기반 API 문서화 추가
- GitHub Actions 기반 CI/CD 적용
- AWS EC2 self-hosted runner 기반 배포 자동화
- Backend Blue-Green 방식 무중단 배포 환경 구성
- Frontend 정적 파일 배포 자동화 구성
- HTTPS 적용 및 Nginx 기반 Reverse Proxy 설정

### 2차 고도화 진행 중

- React SPA 화면 기능 보완
- 인증 방식을 `JWT` 기반으로 전환
- OAuth2 로그인 이후 인증 흐름 정리
- 리뷰, 커뮤니티 등 확장 기능 추가
- 코드 구조 정리 및 리팩토링
- 예외 처리 및 API 응답 형식 개선
- 테스트 코드 보강


## 현재 구현 범위

### 회원/인증

- 로컬 회원가입/로그인
- 세션 기반 인증 처리
- Redis 기반 세션 저장소 적용
- 네이버 OAuth2 로그인
- 구글 OAuth2 로그인 설정
- OAuth2 로그인 성공/실패 후 React 프론트 화면으로 이동 처리
- CORS 및 `withCredentials` 기반 프론트/백엔드 세션 연동
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
- 로그인 판매자 본인 상품 목록 조회
- 판매자/관리자 상품 등록
- 판매자 본인 상품 수정 및 삭제
- 상품 재고 변경
- 구매 처리 시 재고 및 판매 수량 반영
- React 상품 목록/상세 화면과 상품 API 연동

### 장바구니

- 로그인 사용자 장바구니 조회
- 상품 담기
- 상품 수량 변경
- 단일 상품 삭제
- 장바구니 전체 비우기
- 다른 사용자의 장바구니 접근 제한
- React 장바구니 화면과 장바구니 API 연동
- 홈/상품 목록/상품 상세 화면과 장바구니 API 연동

### 주문

- 로그인 사용자 기준 주문 생성
- 장바구니 기반 주문 생성
- 내 주문 목록 조회
- 주문 ID 및 주문번호 기반 주문 조회
- 주문 수령 정보 수정
- 주문 취소
- 관리자 전체 주문 조회
- 관리자 주문 상태 변경
- React 주문서/주문 내역 화면과 주문 API 연동

### 배송

- 관리자 배송 목록 조회
- 배송 상세 조회
- 관리자 또는 주문 사용자 기준 배송 상세 접근 제한
- 판매자/관리자 배송 정보 수정
- 관리자 배송 삭제
- 관리자 배송 관리 화면 구성

### 화면

- React SPA 기반 화면 구성
- React Router 기반 페이지 이동 처리
- 공통 Layout 구성
- React 기반 홈 페이지
- React 기반 상품 목록 페이지
- React 기반 상품 상세 페이지
- React 기반 장바구니 페이지
- React 기반 주문서 페이지
- React 기반 주문 내역 페이지
- React 기반 로그인 페이지
- Axios 기반 API 공통 모듈 구성
- 백엔드 API와 세션 기반 로그인 상태 연동

### API 문서화

- Swagger UI 적용
- OpenAPI 기반 도메인별 API 문서 확인 가능
- User, Product, Cart, Order, Delivery 도메인별 API 태그 구성

### CI/CD 및 배포

- GitHub Actions 기반 CI 구성
- `main` 브랜치 반영 시 CD 실행
- AWS EC2 self-hosted runner 기반 배포 자동화
- Backend Blue-Green 방식 무중단 배포 구성
- Frontend Vite build 후 Nginx 정적 파일 경로로 배포
- HTTPS 적용
- Nginx 기반 Reverse Proxy 설정

## 인프라설계도

<img width="1228" height="802" alt="hmarketver1 5 drawio" src="https://github.com/user-attachments/assets/75785d77-d388-43c1-a48e-02be6652a5d8" />


## 기술 스택

### Backend

- Java 17
- Spring Boot 3.4.3
- Spring Security
- Spring OAuth2 Client
- Spring Session
- Spring Data Redis
- Spring Data JPA
- Spring Validation
- Springdoc OpenAPI
- Lombok
- JWT

### Frontend

- React
- Vite
- React Router
- Axios
- ESLint

### Database

- PostgreSQL
- Redis

### Build / Test

- Gradle
- JUnit 5
- Spring Security Test
- H2 Database

### Infra / DevOps

- AWS EC2
- GitHub Actions
- EC2 self-hosted runner
- Nginx
- HTTPS
- Blue-Green Deployment
