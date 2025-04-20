package com.project.marketplace.user.service;

import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        System.out.println("Access Token: " + userRequest.getAccessToken().getTokenValue());
        System.out.println("Token Type: " + userRequest.getAccessToken().getTokenType().getValue());
        System.out.println("Expires At: " + userRequest.getAccessToken().getExpiresAt());

        // 로그인 서비스 구분 (naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();



        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 네이버는 response 안에 사용자 정보가 있음
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String userName = "naver_" + providerId;

        // 이미 가입된 사용자인지 확인
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(registrationId, providerId);

        if (userOptional.isEmpty()) {
            // 신규 사용자면 정보 저장
            User user = User.builder()
                    .userName(userName)
                    .provider(registrationId)
                    .providerId(providerId)
                    .role("USER") // 기본 권한
                    .build();
            userRepository.save(user);
        }

        // 사용자 권한 정보를 포함한 OAuth2User 객체 생성
        Collection<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new DefaultOAuth2User(
                authorities,
                attributes,
                userNameAttributeName
        );
    }
}