package com.project.marketplace.security;


import com.project.marketplace.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
//    private final OAuth2LoginAuthenticationProvider oAuth2LoginAuthenticationProvider;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI와 OpenAPI 명세는 로그인 없이 확인할 수 있게 허용함
                        .requestMatchers(antMatcher("/swagger-ui.html"), antMatcher("/swagger-ui/**"), antMatcher("/v3/api-docs/**")).permitAll()
                        // 비로그인 사용자도 회원가입 화면과 인증 진입 화면에 접근할 수 있게 허용함
                        .requestMatchers(antMatcher("/"), antMatcher("/shop"), antMatcher("/login"), antMatcher("/signup"), antMatcher("/loginSuccess"), antMatcher("/oauth2/**"), antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/user/signup"), antMatcher(HttpMethod.POST, "/api/user/signin")).permitAll()
                        // 관리자 화면과 사용자 목록 API는 관리자 권한으로 제한함
                        .requestMatchers(antMatcher("/admin/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/user/list")).hasRole("ADMIN")
                        // 내 상품 목록은 공개 상품 조회보다 먼저 제한해 판매자와 관리자만 접근하게 함
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/products/mine")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/products"), antMatcher(HttpMethod.GET, "/api/products/**"), antMatcher(HttpMethod.GET, "/products/**")).permitAll()
                        // 상품 관리 화면과 변경 API는 판매자와 관리자만 접근하도록 제한함
                        .requestMatchers(antMatcher("/seller/**")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/products")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.PUT, "/api/products/**")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/products/**")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/products/**")).hasAnyRole("SELLER", "ADMIN")
                        // 배송 관리 역할 정책을 SecurityConfig로 모아 컨트롤러의 중복 role 검사를 줄임
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/delivery")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.PUT, "/api/delivery/**")).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/delivery/**")).hasRole("ADMIN")
                        // 주문 운영 역할 정책을 SecurityConfig로 모아 컨트롤러의 중복 role 검사를 줄임
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/orders")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/orders/*/status")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/orders"), antMatcher("/api/carts/**"), antMatcher("/api/orders/**")).authenticated()
                        .requestMatchers(antMatcher("/api/user/admin/**")).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        // OAuth2 callback은 기본 /login/oauth2/code/{registrationId}를 사용해 provider별 경로를 자동 매칭함 -5/31
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                )
        ;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
