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
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/products"), antMatcher(HttpMethod.GET, "/api/products/**"), antMatcher(HttpMethod.GET, "/products/**")).permitAll()
                        .requestMatchers(antMatcher("/seller/**"), antMatcher("/orders"), antMatcher("/api/carts/**"), antMatcher("/api/orders/**")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/products")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.PUT, "/api/products/**")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/products/**")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/products/**")).authenticated()
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
