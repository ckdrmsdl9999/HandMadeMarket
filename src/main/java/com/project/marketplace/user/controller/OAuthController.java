package com.project.marketplace.user.controller;

//import com.project.marketplace.security.JwtTokenProvider;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

//    private final JwtTokenProvider jwtTokenProvider;

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

//            // JWT 토큰 생성
//            String token = jwtTokenProvider.createToken(mobile,
//                    (Collection<? extends GrantedAuthority>) authentication.getAuthorities());

            // 모델에 토큰 추가
//            model.addAttribute("token", token);
            model.addAttribute("userName", response.get("name"));

            return "login-success"; // 뷰 이름 반환
        } catch (Exception e) {
            // 예외 처리
            return "redirect:/";
        }
    }
//    // 홈 페이지
//    @GetMapping("/login/oauth2/code/naver")
//    public String home2() {
//        return "login-success"; // home.html 템플릿 반환
//    }
// <a href="/oauth2/authorization/naver/logout">네이버 로그아웃</a>
// 로그아웃
    @GetMapping("/")
        public String home(){
            return "home";
    }


    @PostMapping("/logout/naver")
    public String logoutNaver(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 네이버 액세스 토큰 가져오기
            String accessToken = getAccessTokenFromAuth();
//            if (accessToken == null) {
//                return "redirect:/?error=token_not_found";
//            }

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
            // OAuth2AuthenticationToken에서 액세스 토큰 추출 로직 구현
            // 예: return ((OAuth2AuthenticationToken) authentication).getCredentials();

            // 또는 별도로 저장해둔 토큰 가져오기
            // 예: UserTokenRepository에서 사용자의 토큰 조회
            return "stored_access_token"; // 이 부분은 실제 구현 필요
        }
        return null;
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

