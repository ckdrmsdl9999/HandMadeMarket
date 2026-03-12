package com.project.marketplace.user.controller;

import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {//일단 만들어보자구

    private final UserRepository userRepository;
    // 네이버 토큰 해제 호출과 로그인 설정값을 일치시키기 위해 클라이언트 정보를 yml에서 주입받는다.
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User, Model model, Authentication authentication) {
    // oauth2User가 null인 경우 처리 (직접 URL 접속 시)
        if (oauth2User == null) {
            System.out.println("oauth2User is null!");
            return "redirect:/"; // 홈페이지로 리다이렉트
        }
        System.out.println("oauth2User attributes(/loginsuccess): " + oauth2User.getAttributes());

        try {

            Map<String, Object> attributes = oauth2User.getAttributes();
            Object responseObject = attributes.get("response");
            if (!(responseObject instanceof Map<?, ?> responseRaw)) {
                return "redirect:/";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseRaw;

            String provider = "naver";
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                provider = oauthToken.getAuthorizedClientRegistrationId();
            }
            String providerId = (String) response.get("id");

            model.addAttribute("userName", response.get("name"));
            model.addAttribute("email", response.get("email"));
            model.addAttribute("mobile", response.get("mobile"));
            model.addAttribute("provider", provider);
            model.addAttribute("providerId", providerId);

            // OAuth2 로그인 시 저장된 사용자 토큰을 조회해서 성공 화면에서 확인 가능하게 한다.
            if (providerId != null && !providerId.isBlank()) {
                userRepository.findByProviderAndProviderId(provider, providerId)
                        .ifPresent(user -> {
                            model.addAttribute("token", user.getAccessToken());
                            model.addAttribute("tokenExpiresAt", user.getTokenExpiresAt());
                        });
            }

            return "login-success"; // 뷰 이름 반환
        } catch (Exception e) {
            // 예외 처리
            return "redirect:/";
        }
    }

    @GetMapping("/oauth2/callback")
    public String loginSuccess3(@AuthenticationPrincipal OAuth2User oauth2User, Model model, Authentication authentication) {
    //  oauth2User가 null인 경우 처리 (직접 URL 접속 시)
        if (oauth2User == null) {
            System.out.println("oauth2User is null!");
            return "redirect:/"; // 홈페이지로 리다이렉트
        }
        System.out.println("oauth2User attributes(callback): " + oauth2User.getAttributes());

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

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "login";
    }

    @GetMapping("/oauth2/authoriztion/naver")
    public String redirectTypoOAuthPath() {
        return "redirect:/oauth2/authorization/naver";
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication){
        addAuthInfoToModel(model, authentication);
        return "home";
    }

    // 장바구니 화면도 홈과 동일한 인증 상태 정보를 사용하도록 모델을 채워 반환한다.
    @GetMapping("/cart")
    public String cart(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "cart";
    }

    // 홈 상품 카드에서 전달한 productId를 템플릿에 주입해 상세 페이지가 해당 상품을 조회하도록 연결한다.
    @GetMapping("/products/{productId}")
    public String productDetails(@PathVariable Long productId, Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        model.addAttribute("productId", productId);
        return "productDetails";
    }


    @PostMapping("/logout/naver")
    public String logoutNaver(HttpServletRequest request, HttpServletResponse response) {
        boolean naverLogoutSuccess = true;
        try {

            String accessToken = getAccessTokenFromAuth();

            if (accessToken != null && !accessToken.isBlank()) {
                naverLogoutSuccess = revokeNaverToken(accessToken);
            } else {
                log.warn("액세스 토큰이 없어 네이버 토큰 해제는 건너뛰고 로컬 로그아웃만 수행합니다.");
            }
        } catch (Exception e) {
            log.error("네이버 로그아웃 처리 중 오류 발생", e);
            naverLogoutSuccess = false;
        } finally {
            clearLocalAuthentication(request, response);
        }

        return naverLogoutSuccess ? "redirect:/?logout=success" : "redirect:/?logout=partial";
    }

    private String getAccessTokenFromAuth() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();


            Map<String, Object> attributes = oauth2User.getAttributes();
            Object responseObject = attributes.get("response");
            if (!(responseObject instanceof Map<?, ?> responseRaw)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseRaw;
            String providerId = (String) response.get("id");


            String provider = "naver";
            if (authentication instanceof OAuth2AuthenticationToken) {
                provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            }


            Optional<User> userOpt = userRepository.findByProviderAndProviderId(provider, providerId);
            if (userOpt.isPresent()) {
                String token = userOpt.get().getAccessToken();
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            }


            log.warn("사용자의 액세스 토큰을 찾을 수 없습니다: provider={}, providerId={}", provider, providerId);
            return null;
        }
        return null;
    }

    private void clearTokenInDatabase(String accessToken) {
        try {

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
//            params.add("client_id", "tjdb79ERpbO7fZ0lmU7N");
//            params.add("client_secret", "LzBHj360fR");
            params.add("client_id", naverClientId);
            params.add("client_secret", naverClientSecret);
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
        // 세션 무효화 직전에 SecurityContext를 지워 동일 요청/리다이렉트 구간에서 인증이 남아 보이는 문제를 막는다.
        SecurityContextHolder.clearContext();

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

        // 4. 세션 쿠키도 함께 제거해 브라우저 기준 로그인 흔적을 즉시 정리한다.
        Cookie sessionCookie = new Cookie("JSESSIONID", null);
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);

        // 5. 토큰 블랙리스트에 추가 (옵션)
        // tokenBlacklistService.addToBlacklist(accessToken);
    }

    private void addAuthInfoToModel(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getPrincipal());

        model.addAttribute("isLoggedIn", isLoggedIn);
        // 로그인 사용자 ID를 전달해서 이용
        model.addAttribute("currentUserId", isLoggedIn ? resolveCurrentUserId(authentication) : null);

        if (!isLoggedIn) {
            return;
        }

        model.addAttribute("authName", authentication.getName());

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {

            OAuth2User oauth2User = oauthToken.getPrincipal();
            model.addAttribute("provider", oauthToken.getAuthorizedClientRegistrationId());

            Map<String, Object> attributes = oauth2User.getAttributes();
            Object responseObj = attributes.get("response");
            if (responseObj instanceof Map<?, ?> response) {
                Object name = response.get("name");
                Object email = response.get("email");
                model.addAttribute("displayName", name != null ? name : authentication.getName());
                model.addAttribute("email", email);
                return;
            }
        }


        model.addAttribute("displayName", authentication.getName());
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            Object responseObj = oauthToken.getPrincipal().getAttributes().get("response");
            if (responseObj instanceof Map<?, ?> response) {
                Object providerId = response.get("id");
                if (providerId instanceof String providerIdText && !providerIdText.isBlank()) {
                    return userRepository.findByProviderAndProviderId(
                                    oauthToken.getAuthorizedClientRegistrationId(),
                                    providerIdText
                            )
                            .map(User::getUserId)
                            .orElse(null);
                }
            }
        }

        return userRepository.findByUserName(authentication.getName())
                .map(User::getUserId)
                .orElse(null);
    }
}
