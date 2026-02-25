package com.project.marketplace.user.service;

import com.project.marketplace.user.entity.User;
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
        //String userName = "naver_" + providerId;

        //하드코딩제거
        String userName = registrationId + "_" + providerId;

        // 이미 가입된 사용자인지 확인
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(registrationId, providerId);
        User user;

        if (userOptional.isEmpty()) {
            // 신규 사용자 생성 및 토큰 저장
            user = User.builder()
                    .userName(userName)
                    .email(email)
                    .provider(registrationId)
                    .providerId(providerId)
                    .accessToken(accessToken)
                    .tokenExpiresAt(expiresAt)
                    .role("USER") // 기본 권한
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
        }

        // 사용자 저장 또는 업데이트
        userRepository.save(user);

        // 사용자 권한 정보를 포함한 OAuth2User 객체 생성
        Collection<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new DefaultOAuth2User(
                authorities,
                attributes,
                userNameAttributeName
        );
    }
}
