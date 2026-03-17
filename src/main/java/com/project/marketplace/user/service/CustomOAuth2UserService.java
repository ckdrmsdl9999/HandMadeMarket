package com.project.marketplace.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.security.oauth2.core.OAuth2Error;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    // OAuth2 디버그 데이터를 JSON 문자열로 통일해 다음 단계 로그 추가를 쉽게 맞춤 -3/17
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 어떤 OAuth2UserService 구현체가 실제로 호출되는지 확인하기 위한 로그
        log.info("[OAuth2] loadUser called - serviceClass={}, registrationId={}",
                this.getClass().getName(),
                userRequest.getClientRegistration().getRegistrationId());

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 액세스 토큰 정보
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String clientRegistration = userRequest.getClientRegistration().toString();
       String additionalParameters = userRequest.getAdditionalParameters().toString();
        String tokenType = userRequest.getAccessToken().getTokenType().getValue();
        LocalDateTime expiresAt = null;

        // 만료 시간이 있는 경우 LocalDateTime으로 변환
        if (userRequest.getAccessToken().getExpiresAt() != null) {
            expiresAt = LocalDateTime.ofInstant(
                    userRequest.getAccessToken().getExpiresAt(),
                    ZoneId.systemDefault()
            );
        }
//
//        System.out.println("Access Token2: " + accessToken);
//        System.out.println("Token Type2: " + tokenType);
//        System.out.println("Expires At2: " + expiresAt);
//        System.out.println("clientRegistration2: " + clientRegistration);
//        System.out.println("additionalParameters2: " + additionalParameters);


//        log.debug("[OAuth2] token metadata - type={}, expiresAt={}, registration={}",
//                tokenType, expiresAt, clientRegistration);

        log.debug("[OAuth2] token debug - accessToken={}, additionalParameters={}, type={}, expiresAt={}, registration={}",
                accessToken, additionalParameters, tokenType, expiresAt, clientRegistration);

        // 로그인 서비스 구분 (naver,google등..)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
//
        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();


//        // 네이버는 response 안에 사용자 정보가 있음
//        Map<String, Object> attributes = oAuth2User.getAttributes();
//        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        //response/providerId 필수값 검증 추가
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Object responseObject = attributes.get("response");
        if (!(responseObject instanceof Map<?, ?> responseRaw)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "OAuth2 사용자 정보(response)를 찾을 수 없습니다."
            );
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) responseRaw;

        String providerId = (String) response.get("id");
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "OAuth2 사용자 식별자(providerId)가 없습니다."
            );
        }

//      String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String name = (String) response.get("name");

        String userName = (name != null && !name.isBlank()) ? name : registrationId + "_" + providerId;

        Optional<User> userOptional = userRepository.findByProviderAndLoginId(registrationId, providerId);
        User user;

        if (userOptional.isEmpty()) {
            // 신규 사용자 생성 및 토큰 저장
            user = User.builder()
                    .loginId(providerId)
                    .userName(userName)
                    .email(email)
                    .provider(registrationId)
                    .accessToken(accessToken)
                    .tokenExpiresAt(expiresAt)
                    .role(UserRole.USER) // 기본 권한
                    .build();
        } else {
            // 기존 사용자면 토큰 정보 업데이트
            user = userOptional.get();
            user.setAccessToken(accessToken);
            user.setTokenExpiresAt(expiresAt);

            // 이메일이 변경되었거나 없는 경우 업데이트
            if (email != null && (user.getEmail() == null || !email.equals(user.getEmail()))) {
                user.setEmail(email);
            }
            // 소셜 프로필 이름이 있으면 표시 이름도 최신 상태로 갱신한다 -3/16
            if (name != null && !name.isBlank()) {
                user.setUserName(name);
            }
        }

        // 사용자 저장 또는 업데이트
        userRepository.save(user);

        Collection<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new DefaultOAuth2User(
                authorities,
                attributes,
                userNameAttributeName
        );
    }

    // OAuth2 흐름에서 받은 객체를 같은 형식으로 남겨 단계별 비교가 쉬워지게 추가함 -3/17
    private void logJson(String label, Object payload) {
        try {
            log.info("[OAuth2] {}={}", label, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[OAuth2] {}={}", label, payload);
        }
    }
}
