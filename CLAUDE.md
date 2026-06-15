# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HandMadeMarket (수제상품 판매 커뮤니티) — a handmade product marketplace built with Spring Boot. Currently in early development (MVP phase).

## Build & Run Commands

```bash
./gradlew build          # Build the project
./gradlew bootRun        # Run the application (port 8080)
./gradlew test           # Run all tests (JUnit 5)
./gradlew jmeter         # Run JMeter performance tests (results in build/jmeter-results/)
./gradlew clean build    # Clean and rebuild
```

Single test class: `./gradlew test --tests "com.project.marketplace.SomeTestClass"`

## Tech Stack

- **Java 17**, **Spring Boot 3.4.3**, **Gradle 8.12.1**
- **PostgreSQL** on `localhost:5432/marketplace` (user: `postgres`)
- **Spring Data JPA** (primary data access, transitioning from MyBatis)
- **MyBatis** (legacy, still used in Order module via `OrderMapper`)
- **Spring Security** with Naver OAuth2 login
- **JWT** (configured but `JwtTokenProvider` currently commented out)
- **Thymeleaf** for test frontend templates
- **Lombok** used throughout (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`, etc.)

## Architecture

Layered architecture under `com.project.marketplace`:

| Package | Purpose |
|---------|---------|
| `product/` | Product CRUD, search, popular products (JPA) |
| `order/` | Order management (MyBatis mapper — migration to JPA pending) |
| `user/` | User accounts, Naver OAuth2 login/logout, token management |
| `cart/` | Shopping cart |
| `delivery/` | Delivery/shipping tracking |
| `security/` | SecurityConfig, OAuth2 success handler, JWT filter |
| `config/` | Session config, RestTemplate config |

Each module follows: **Controller → Service → Repository/Mapper**, with **DTOs** for API payloads and **Entities** for persistence.

## Key Patterns

- **DTO ↔ Entity conversion**: DTOs have `fromEntity()` (static) and `toEntity()` instance methods
- **Dependency injection**: Constructor injection via Lombok `@RequiredArgsConstructor`
- **Transactions**: `@Transactional` on service methods, `@Transactional(readOnly = true)` for reads
- **Custom JPA queries**: JPQL via `@Query` in repositories (e.g., `ProductRepository.findPopularProducts()`)

## Authentication Flow

- Naver OAuth2 is the primary login method
- `CustomOAuth2UserService` handles user creation/update and stores access tokens in DB
- `OAuth2AuthenticationSuccessHandler` sets session attributes and redirects
- Logout via `/logout/naver` revokes token from Naver's server and clears session
- Session policy: `IF_REQUIRED`; CSRF, form login, HTTP basic all disabled

## Database

- **Hibernate DDL**: `create-drop` (schema recreated on each startup — dev mode)
- **Entities**: User, Product, Delivery, Order (Order still uses MyBatis XML-less mapper)
- No migration tool (Flyway/Liquibase) — schema managed by Hibernate auto-DDL

## Notes

- The project is actively transitioning from MyBatis to JPA (recent refactoring commits)
- Several classes are partially commented out (`AuthController`, `JwtTokenProvider`, `SessionConfig`) as auth strategy evolves
- Commit messages are in Korean; follow the existing convention (`feat:`, `fix:`, `refactor:`)
