package com.project.marketplace.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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

        // 네이버 로그인 정보 추출
        if ("naver".equals(registrationId) && attributes.containsKey("response")) {
            Map<String, Object> response_attr = (Map<String, Object>) attributes.get("response");

            // 세션에 필요한 정보 저장
            HttpSession session = request.getSession(true);
            session.setAttribute("name", response_attr.get("name"));
            session.setAttribute("email", response_attr.get("email"));
            session.setAttribute("providerId", response_attr.get("id"));
            session.setAttribute("provider", registrationId);
            session.setAttribute("mobile", response_attr.get("mobile"));

            // 로그인 토큰 값을 저장 (로그아웃 시 사용)
            // 실제 토큰 값은 OAuth2AuthorizedClientService에서 가져와야 합니다
            // session.setAttribute("accessToken", token);

            log.info("세션에 사용자 정보 저장 완료: {}", response_attr.get("name"));
        }

        // 기본 성공 URL로 리다이렉트
        super.onAuthenticationSuccess(request, response, authentication);
    }
}