package com.project.marketplace.user.service;


import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * 사용자 ID로 유효한 액세스 토큰 조회
     */
    @Transactional(readOnly = true)
    public Optional<String> getValidAccessToken(Long userId) {
        return userRepository.findById(userId)
                .filter(this::isTokenValid)
                .map(User::getAccessToken);
    }

    /**
     * 공급자(provider)와 공급자ID로 유효한 액세스 토큰 조회
     */
    @Transactional(readOnly = true)
    public Optional<String> getValidAccessToken(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .filter(this::isTokenValid)
                .map(User::getAccessToken);
    }

    /**
     * 토큰 유효성 검사
     */
    private boolean isTokenValid(User user) {
        return user.getAccessToken() != null &&
                user.getTokenExpiresAt() != null &&
                LocalDateTime.now().isBefore(user.getTokenExpiresAt());
    }

    /**
     * 네이버 API 호출 예시 메서드
     */
    public ResponseEntity<String> callNaverApi(String endpoint, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                entity,
                String.class
        );
    }

    /**
     * 네이버 토큰 삭제 요청
     */
    public boolean revokeNaverToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "delete");
            params.add("client_id", "tjdb79ERpbO7fZ0lmU7N");  // 설정에서 가져오는 것이 좋음
            params.add("client_secret", "LzBHj360fR");  // 설정에서 가져오는 것이 좋음
            params.add("access_token", accessToken);
            params.add("service_provider", "NAVER");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://nid.naver.com/oauth2.0/token", entity, String.class);

            log.info("네이버 토큰 삭제 응답: {}", response.getBody());

            // 토큰이 성공적으로 삭제되었다면 DB에서도 토큰 정보 제거
            if (response.getStatusCode().is2xxSuccessful()) {
                clearTokenInfo(accessToken);
            }

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("네이버 토큰 삭제 중 오류", e);
            return false;
        }
    }

    /**
     * DB에서 해당 토큰 정보 제거
     */
    @Transactional
    public void clearTokenInfo(String accessToken) {
        userRepository.findAll().stream()
                .filter(user -> accessToken.equals(user.getAccessToken()))
                .forEach(user -> {
                    user.setAccessToken(null);
                    user.setTokenExpiresAt(null);
                    userRepository.save(user);
                });
    }
}