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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    // OAuth2 성공 직후 세션과 인증값을 JSON으로 같은 형식에 찍을 준비를 맞춤 -3/17
    private final ObjectMapper objectMapper;

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

        // 네이버 로그인 정보 추출
        if ("naver".equals(registrationId) && attributes.containsKey("response")) {
            Map<String, Object> response_attr = (Map<String, Object>) attributes.get("response");
            // 필요한 정보 추출
            String providerId = (String) response_attr.get("id");
            String name = (String) response_attr.get("name");
            String mobile = (String) response_attr.get("mobile");

            // 세션에 필요한 정보 저장
            HttpSession session = request.getSession(true);
            session.setAttribute("name", response_attr.get("name"));
            session.setAttribute("providerId", response_attr.get("id"));
            session.setAttribute("provider", registrationId);
            session.setAttribute("mobile", response_attr.get("mobile"));


            // DB에 사용자 정보 저장 또는 업데이트
            saveOrUpdateUser(registrationId, providerId, name);

            log.info("세션에 사용자 정보 저장 완료: {}", name);
            log.info("DB에 사용자 정보 저장 완료: {}", name);

            // 로그인 토큰 값을 저장 (로그아웃 시 사용)
            // 실제 토큰 값은 OAuth2AuthorizedClientService에서 가져와야 합니다
            // session.setAttribute("accessToken", token);

            log.info("세션에 사용자 정보 저장 완료: {}", response_attr.get("name"));
        }

        // 기본 성공 URL로 리다이렉트
//        super.onAuthenticationSuccess(request, response, authentication);
        response.sendRedirect("/loginSuccess");
    }

    private void saveOrUpdateUser(String provider, String providerId, String name) {
        try {
            // 소셜 사용자 매칭을 provider와 loginId 조합으로 통일해 User 구조와 맞췄다 -3/16
            Optional<User> existingUser = userRepository.findByProviderAndLoginId(provider, providerId);

            if (existingUser.isPresent()) {
                // 기존 사용자 정보 업데이트
                User user = existingUser.get();
                // 필요한 정보 업데이트 (이름, 이메일 등)

                user.setUserName(name);

                userRepository.save(user);
                log.info("기존 사용자 정보 업데이트: {}", name);
            } else {
                // 새 사용자 생성
                // 신규 소셜 사용자는 loginId에 제공자 식별값을 저장하고 userName은 표시 이름으로 둔다 -3/16
                String userName = (name != null && !name.isBlank()) ? name : provider + "_" + providerId;

                User newUser = User.builder()
                        .loginId(providerId)
                        .userName(userName)
                        .provider(provider)
                        .role(UserRole.USER) // 기본 권한
                        .build();

                userRepository.save(newUser);
                log.info("새 사용자 등록: {}", name);
            }
        } catch (Exception e) {
            log.error("사용자 정보 저장 중 오류 발생: {}", e.getMessage(), e);
        }
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
