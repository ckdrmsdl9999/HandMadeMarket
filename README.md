# 수제상품 판매 쇼핑몰 프로젝트 소개(이름 : 핸드메이드스토리)

- 수제상품을 판매할 수 있는 판매 + 커뮤니티 플랫폼입니다.

- 해당 플랫폼은 구매자와 판매자가 서로 소통하고 정보를 공유할 수 있는 공간을 제공합니다.
- 쇼핑몰 필수 기능들을 구현해본 MVP 프로젝트입니다.
- 이 프로젝트는 백엔드 부분을 담당하며 모놀리식으로 구성하였습니다. 우선 쇼핑몰의 주목적인 핵심 주문기능을 구현할 예정이며 구현할 내용은 유저, 주문, 서비스구현 후 1차MVP 버전 배포예정입니다. 추후에 커뮤니티부분, 쇼핑몰 고도화,
  기타 부가기능 확장(2차 구현) 예정입니다. 개발을 마친 뒤 MSA구조로 전환까지 고려중입니다.

## Swagger, Graphiql 📝

- 구현 예정입니다

- [Swagger Rest Api(Only Auth service)](https://clientbe.gongik.shop/swagger-ui/index.html)

## 제작 기간 📅 && 참여 인원 🧑‍🤝‍🧑

- 2025.03.01~
- 취업을 위한 개인 프로젝트입니다.

## 주요 기능 ✨

#### 밑에 api별로 상세 설명이 있으니 여기선 큰 틀에서 설명하겠습니다.

1. 회원가입후 상품 검색, 상품에 대한 정보와 후기공유
2. 상품 주문 기능
3. 신고 기능 (사용자신고, 글신고 등등 다양)
4  커뮤니티를 통해 카테고리별로 소통, 커뮤니티 게시판의 대댓글 기능 까지 가능
5. 본인글에 댓글, 혹은 대댓글 알림 
6. 본인의 카테고리나 검색 이력을 통해 상품 추천기능

## 시스템 구성도 🗺️

예정

## 기술 스택 🧑‍💻

#### Back-End

- Java 17
- Spring Boot 3.2.5
- Spring Security (jwt)
- Spring Batch
- Spring Data JPA
- Spring Data Redis
- Websocket
- Jmeter

#### Data Storage & Processing

- PostgreSQL
- Redis

#### Distributed Tracing

- ElasticSearch
- Logstash
- Kibana
  
#### Authentication
- Oauth2, JWT인증

#### Monitoring
- Jmeter
- Prometheus
- Grafana

####Frontend
-React

####Authentication
Oauth2 + JWT토큰 인증 구현

### Deployment

- Docker compose
- Jenkins
