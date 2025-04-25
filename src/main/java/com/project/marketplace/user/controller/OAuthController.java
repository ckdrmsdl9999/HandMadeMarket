package com.project.marketplace.user.controller;

import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final UserRepository userRepository;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User,
                               Model model,
                               Authentication authentication) {

//         oauth2User가 null인 경우 처리 (직접 URL 접속 시)
        if (oauth2User == null) {
            System.out.println("oauth2User is null!");
            return "redirect:/"; // 홈페이지로 리다이렉트
        }
        System.out.println("oauth2User attributes: " + oauth2User.getAttributes());

        try {
            // OAuth2User에서 사용자 정보 추출
            Map<String, Object> attributes = oauth2User.getAttributes();
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            String mobile = (String) response.get("mobile");

            // 모델에 토큰 추가
//            model.addAttribute("token", token);
            model.addAttribute("userName", response.get("name"));

            return "login-success"; // 뷰 이름 반환
        } catch (Exception e) {
            // 예외 처리
            return "redirect:/";
        }
    }

    @GetMapping("/")
    public String home(){
        return "home";
    }

    @PostMapping("/logout/naver")
    public String logoutNaver(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 네이버 액세스 토큰 가져오기
            String accessToken = getAccessTokenFromAuth();
            if (accessToken == null) {
                return "redirect:/?error=token_not_found";
            }

            // 네이버 토큰 삭제 요청
            boolean naverLogoutSuccess = revokeNaverToken(accessToken);

            // 로컬 인증 정보 삭제
            clearLocalAuthentication(request, response);

            if (naverLogoutSuccess) {
                return "redirect:/?logout=success";
            } else {
                return "redirect:/?logout=partial";
            }
        } catch (Exception e) {
            log.error("네이버 로그아웃 처리 중 오류 발생", e);
            return "redirect:/?error=logout_failed";
        }
    }

    private String getAccessTokenFromAuth() {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // 사용자 정보에서 providerId 추출
            Map<String, Object> attributes = oauth2User.getAttributes();
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            String providerId = (String) response.get("id");

            // 인증 정보에서 provider 값 가져오기 (네이버)
            String provider = "naver";
            if (authentication instanceof OAuth2AuthenticationToken) {
                provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            }

            // DB에서 해당 사용자의 토큰 조회
            Optional<User> userOpt = userRepository.findByProviderAndProviderId(provider, providerId);
            if (userOpt.isPresent()) {
                String token = userOpt.get().getAccessToken();
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            }

            // DB에 토큰이 없는 경우 null 반환
            log.warn("사용자의 액세스 토큰을 찾을 수 없습니다: provider={}, providerId={}", provider, providerId);
            return null;
        }
        return null;
    }

    private void clearTokenInDatabase(String accessToken) {
        try {
            // 액세스 토큰으로 사용자 찾기
            Optional<User> userOpt = userRepository.findByAccessToken(accessToken);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAccessToken(null);
                user.setTokenExpiresAt(null);
                userRepository.save(user);
                log.info("사용자 토큰 정보 삭제 완료: userId={}", user.getUserId());
            } else {
                log.warn("해당 액세스 토큰을 가진 사용자를 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("DB에서 토큰 삭제 중 오류", e);
        }
    }


    private boolean revokeNaverToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "delete");
            params.add("client_id", "tjdb79ERpbO7fZ0lmU7N");  // 설정에서 가져옴
            params.add("client_secret", "LzBHj360fR");  // 설정에서 가져옴
            params.add("access_token", accessToken);
            params.add("service_provider", "NAVER");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://nid.naver.com/oauth2.0/token", entity, String.class);

            log.info("네이버 로그아웃 응답: {}", response.getBody());

            // 토큰이 성공적으로 삭제되었다면 DB에서도 토큰 정보 제거
            if (response.getStatusCode().is2xxSuccessful()) {
                clearTokenInDatabase(accessToken);
            }

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("네이버 토큰 삭제 중 오류", e);
            return false;
        }
    }



    private void clearLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
        // 1. Spring Security 컨텍스트 정리
//        SecurityContextHolder.clearContext();

        // 2. 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 3. JWT 사용 시 쿠키에서 JWT 토큰 제거
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        // 4. 토큰 블랙리스트에 추가 (옵션)
        // tokenBlacklistService.addToBlacklist(accessToken);
    }
}