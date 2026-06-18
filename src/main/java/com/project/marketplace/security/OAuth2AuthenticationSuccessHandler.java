package com.project.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    // OAuth2 성공 직후 세션과 인증값을 JSON으로 같은 형식에 찍을 준비를 맞춤 -3/17
    private final ObjectMapper objectMapper;

    // OAuth2 성공 후 백엔드 홈이 아니라 React 화면으로 돌아가게 프론트 주소를 설정값으로 분리함
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("OAuth2 인증 성공!");
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        // 인증 정보 로깅
        log.info("OAuth2 Provider: {}", registrationId);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.debug("OAuth2 User attributes: {}", attributes);
        log.debug("Oauth2token :{} ", oauthToken);

        // 인증 완료 직후 principal과 attributes를 JSON으로 남겨 success 단계 입력값을 바로 확인하게 추가함 -3/17
        Map<String, Object> authPayload = new LinkedHashMap<>();
        authPayload.put("registrationId", registrationId);
        authPayload.put("principalName", authentication.getName());
        authPayload.put("authorities", authentication.getAuthorities());
        authPayload.put("attributes", attributes);
        logJson("success.authentication", authPayload);

        // 네이버 동의 항목을 이름/이메일로 맞춰 세션 값에서도 휴대폰 정보를 제외함
        OAuth2Profile profile = resolveOAuth2Profile(attributes);
        if (profile.providerId() != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute("name", profile.name());
            session.setAttribute("email", profile.email());
            session.setAttribute("providerId", profile.providerId());
            session.setAttribute("provider", registrationId);

            // 세션 확인 로그도 실제 저장하는 이름/이메일 중심으로 맞춤
            Map<String, Object> sessionPayload = new LinkedHashMap<>();
            sessionPayload.put("sessionId", session.getId());
            sessionPayload.put("name", profile.name());
            sessionPayload.put("email", profile.email());
            sessionPayload.put("providerId", profile.providerId());
            sessionPayload.put("provider", registrationId);
            logJson("success.session", sessionPayload);

            // DB 저장도 provider 공통 profile 기준으로 처리
            saveOrUpdateUser(registrationId, profile.providerId(), profile.name(), profile.email());

            log.info("세션에 사용자 정보 저장 완료: {}", profile.name());
            log.info("DB에 사용자 정보 저장 완료: {}", profile.name());
        } else {
            // provider별 응답에서 식별자를 찾지 못하면 사용자 저장 없이 흐름만 기록
            log.warn("OAuth2 사용자 식별자를 찾을 수 없습니다: provider={}", registrationId);
        }

        // OAuth2 성공 후 백엔드 홈이 아니라 React 홈 화면으로 이동하게 수정함
        String redirectUrl = buildFrontendRedirectUrl("/");
        logJson("success.redirect", Map.of("location", redirectUrl));
        response.sendRedirect(redirectUrl);
    }

    // OAuth2 사용자 저장도 정규화 profile 값과 email을 함께 반영
    private void saveOrUpdateUser(String provider, String providerId, String name, String email) {
        try {
            // 소셜 사용자 매칭을 provider와 loginId 조합으로 통일해 User 구조와 맞췄다 -3/16
            Optional<User> existingUser = userRepository.findByProviderAndLoginId(provider, providerId);
            String userName = (name != null && !name.isBlank()) ? name : provider + "_" + providerId;

            if (existingUser.isPresent()) {
                // 기존 소셜 사용자도 정규화된 이름/email 기준으로 갱신
                User user = existingUser.get();
                user.setUserName(userName);
                if (email != null) {
                    user.setEmail(email);
                }

                userRepository.save(user);
                log.info("기존 사용자 정보 업데이트: {}", userName);
            } else {
                // 신규 소셜 사용자도 providerId를 loginId로 저장하고 표시 이름/email을 함께 저장
                User newUser = User.builder()
                        .loginId(providerId)
                        .userName(userName)
                        .email(email)
                        .provider(provider)
                        .role(UserRole.USER) // 기본 권한
                        .build();

                userRepository.save(newUser);
                log.info("새 사용자 등록: {}", userName);
            }
        } catch (Exception e) {
            log.error("사용자 정보 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private OAuth2Profile resolveOAuth2Profile(Map<String, Object> attributes) {
        // 네이버/구글 공통으로 쓰는 식별자, 이름, 이메일만 profile로 정규화함
        String providerId = asText(attributes.get("providerId"));
        String name = asText(attributes.get("name"));
        String email = asText(attributes.get("email"));

        Object responseObj = attributes.get("response");
        if (responseObj instanceof Map<?, ?> response) {
            providerId = firstNonBlank(providerId, asText(response.get("id")));
            name = firstNonBlank(name, asText(response.get("name")));
            email = firstNonBlank(email, asText(response.get("email")));
        }

        providerId = firstNonBlank(providerId, asText(attributes.get("sub")));
        return new OAuth2Profile(providerId, name, email);
    }

    private String firstNonBlank(String value, String fallback) {
        // 앞에서 빈 문자열을 null로 정리했으므로 null 여부만 보고 fallback을 선택
        return value != null ? value : fallback;
    }

    private String asText(Object value) {
        // provider 응답 값 타입 차이를 문자열 또는 null로 통일함
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private record OAuth2Profile(String providerId, String name, String email) {
    }

    // frontendUrl 끝 슬래시와 path 시작 슬래시가 겹치지 않도록 정리함
    private String buildFrontendRedirectUrl(String path) {
        String baseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        return baseUrl + path;
    }

    // OAuth2 성공 처리 중 객체 로그를 JSON 한 형태로 남기기 쉽게 추가함 -3/17
    private void logJson(String label, Object payload) {
        try {
            log.info("[OAuth2] {}={}", label, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[OAuth2] {}={}", label, payload);
        }
    }

}
