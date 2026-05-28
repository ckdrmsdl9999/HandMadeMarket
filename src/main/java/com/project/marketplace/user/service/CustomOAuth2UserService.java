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

        //dfeaultOAuth2UserService의 loadUser 실행해줌으로써 네이버에서 사용자정보 api호출후 저장(안하면 안받아오니까)
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
        // OAuth2 토큰과 추가 파라미터를 JSON 한 묶음으로 남겨 실제 요청 데이터를 바로 비교하게 추가함 -3/17
        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("registrationId", userRequest.getClientRegistration().getRegistrationId());
        requestPayload.put("accessToken", accessToken);
        requestPayload.put("tokenType", tokenType);
        requestPayload.put("expiresAt", expiresAt);
        requestPayload.put("additionalParameters", userRequest.getAdditionalParameters());
        requestPayload.put("userRequest당", userRequest);
        logJson("loadUser.userRequest", requestPayload);


        log.debug("[OAuth2] token debug - accessToken={}, additionalParameters={}, type={}, expiresAt={}, registration={}",
                accessToken, additionalParameters, tokenType, expiresAt, clientRegistration);

        // 로그인 서비스 구분 (naver,google등..)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
//
        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();


        Map<String, Object> attributes = new LinkedHashMap<>(oAuth2User.getAttributes());
        OAuth2Profile profile = extractProfile(registrationId, attributes);

        String providerId = profile.providerId();
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "OAuth2 사용자 식별자(providerId)가 없습니다."
            );
        }



        //usernameAttributesName="response" 이므로 value값은 response값
        Object nameAttributeValue = attributes.get(userNameAttributeName);
        // user-name-attribute 기준값과 response 식별값을 같이 남겨 principal 이름 기준을 바로 확인하게 로그 추가함 -5/14
        Map<String, Object> principalPayload = new LinkedHashMap<>();
        principalPayload.put("userNameAttributeName", userNameAttributeName);//"response"출력
        principalPayload.put("nameAttributeValue", nameAttributeValue);
        principalPayload.put("nameAttributeValueType", nameAttributeValue != null ? nameAttributeValue.getClass().getName() : null);
        principalPayload.put("responseId", providerId);
      //  principalPayload.put("responseName", response.get("name"));
        principalPayload.put("expectedPrincipalName", nameAttributeValue != null ? nameAttributeValue.toString() : null);
        logJson("loadUser.principal", principalPayload);

//      String providerId = (String) response.get("id");
        String email = profile.email();
        String name = profile.name();

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

        // DB 반영 뒤 사용자 식별값과 토큰 저장 결과를 JSON으로 남겨 화면 값과 대조하기 쉽게 추가
        Map<String, Object> savedUserPayload = new LinkedHashMap<>();
        savedUserPayload.put("dbUserId", user.getId());
        savedUserPayload.put("provider", user.getProvider());
        savedUserPayload.put("loginId", user.getLoginId());
        savedUserPayload.put("userName", user.getUserName());
        savedUserPayload.put("email", user.getEmail());
        savedUserPayload.put("accessToken", user.getAccessToken());
        savedUserPayload.put("tokenExpiresAt", user.getTokenExpiresAt());
        logJson("loadUser.savedUser", savedUserPayload);

        Collection<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // OAuth2 principal 이름을 공급자별 원본 키 대신 공통 식별값으로 맞춤 -5/28
        attributes.put("provider", registrationId);
        attributes.put("providerId", providerId);
        attributes.put("principalName", registrationId + ":" + providerId);

        return new DefaultOAuth2User(
                authorities,
                attributes,
                "principalName"
        );

    }

    // OAuth2 공급자별 사용자 응답 구조 차이를 providerId/email/name으로 정규화함 -5/28
    private OAuth2Profile extractProfile(String registrationId, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            Object responseObject = attributes.get("response");
            logJson("loadUser.responseRaw", responseObject);

            if (!(responseObject instanceof Map<?, ?> responseRaw)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_user_info"),
                        "OAuth2 사용자 정보(response)를 찾을 수 없습니다."
                );
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseRaw;

            logJson("loadUser.response", response);

            return new OAuth2Profile(
                    (String) response.get("id"),
                    (String) response.get("email"),
                    (String) response.get("name")
            );
        }

        if ("google".equals(registrationId)) {
            return new OAuth2Profile(
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name")
            );
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error("unsupported_provider"),
                "지원하지 않는 OAuth2 provider입니다: " + registrationId
        );
    }

    private record OAuth2Profile(String providerId, String email, String name) {

    }

    // OAuth2 흐름에서 받은 객체를 같은 형식으로 남겨 단계별 비교가 쉬워지게 추가
    private void logJson(String label, Object payload) {
        try {
            log.info("[OAuth2] {}={}", label, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[OAuth2] {}={}", label, payload);
        }
    }
}
