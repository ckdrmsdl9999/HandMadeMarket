package com.project.marketplace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import com.project.marketplace.user.service.UserService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {//일단 만들어보자구

    private final UserRepository userRepository;
    private final UserService userService;
    // OAuth2 화면 전환과 로그아웃 데이터를 JSON 로그로 같은 방식에 확인하게 맞춤 -3/17
    private final ObjectMapper objectMapper;
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
            // 로그인 성공 화면 값을 provider 공통 attributes와 DB 사용자 기준으로 구성
            String provider = resolveProvider(authentication, attributes);
            String providerId = (String) attributes.get("providerId");
            Object name = resolveOAuthAttribute(oauth2User, "name");
            Object email = resolveOAuthAttribute(oauth2User, "email");
            Object mobile = resolveOAuthAttribute(oauth2User, "mobile");
            Optional<User> currentUser = userService.getAuthenticatedUser(authentication);

            // 로그인 성공 화면으로 넘어온 원본 attributes를 JSON으로 남겨 successHandler 다음 값을 바로 확인하게 맞춤
            logJson("controller.loginSuccess.attributes", attributes);

            model.addAttribute("userName", name);
            model.addAttribute("email", email);
            model.addAttribute("mobile", mobile);
            model.addAttribute("provider", provider);
            model.addAttribute("providerId", providerId);

            // OAuth2 로그인 시 저장된 사용자 토큰도 공통 인증 사용자 조회 결과에서 가져옴
            currentUser.ifPresent(user -> {
                model.addAttribute("token", user.getAccessToken());
                model.addAttribute("tokenExpiresAt", user.getTokenExpiresAt());
            });

            // 화면에 전달한 최종 모델 값을 JSON으로 남겨 템플릿에서 보는 값과 대조하기 쉽게 맞춤
            Map<String, Object> modelPayload = new LinkedHashMap<>();
            modelPayload.put("userName", name);
            modelPayload.put("email", email);
            modelPayload.put("mobile", mobile);
            modelPayload.put("provider", provider);
            modelPayload.put("providerId", providerId);
            currentUser.ifPresent(user -> {
                modelPayload.put("token", user.getAccessToken());
                modelPayload.put("tokenExpiresAt", user.getTokenExpiresAt());
            });
            logJson("controller.loginSuccess.model", modelPayload);

            return "login-success"; // 뷰 이름 반환
        } catch (Exception e) {
            // 예외 처리
            return "redirect:/";
        }
    }

    @GetMapping("/oauth2/callback")
    public String loginSuccess3(@AuthenticationPrincipal OAuth2User oauth2User, Model model, Authentication authentication) {
        // callback 화면 처리도 loginSuccess와 같은 공통 OAuth2 모델 구성을 사용
        return loginSuccess(oauth2User, model, authentication);
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "login";
    }

    // 상품 등록 MVP 화면 진입 경로를 추가
    @GetMapping("/seller/products/new")
    public String productForm(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (!isLoggedIn) {
            return "redirect:/login";
        }

        addAuthInfoToModel(model, authentication);
        return "product-form";
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
            // 로그아웃 시작 시 현재 토큰 보유 여부와 토큰 값을 JSON으로 남겨 요청 출발 지점을 확인하게 추가함 -3/17
            Map<String, Object> logoutStartPayload = new LinkedHashMap<>();
            logoutStartPayload.put("hasAccessToken", accessToken != null && !accessToken.isBlank());
            logoutStartPayload.put("accessToken", accessToken);
            logJson("logout.start", logoutStartPayload);

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
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return null;
        }

        if (!"naver".equals(oauthToken.getAuthorizedClientRegistrationId())) {
            return null;
        }

        // 네이버 토큰 해제용 accessToken도 공통 인증 사용자 조회 결과에서 가져옴
        Optional<String> accessToken = userService.getAuthenticatedUser(authentication)
                .map(User::getAccessToken)
                .filter(token -> token != null && !token.isBlank());

        if (accessToken.isEmpty()) {
            log.warn("사용자의 네이버 액세스 토큰을 찾을 수 없습니다: authName={}", authentication.getName());
        }

        return accessToken.orElse(null);
    }


    private void clearTokenInDatabase(String accessToken) {
        try {

            Optional<User> userOpt = userRepository.findByAccessToken(accessToken);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAccessToken(null);
                user.setTokenExpiresAt(null);
                userRepository.save(user);
                // 토큰 삭제 로그도 현재 로그인 식별자 필드명에 맞춰 남기도록 변경했다 -3/16
                log.info("사용자 토큰 정보 삭제 완료: loginId={}", user.getLoginId());
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

            // 네이버 토큰 해제 호출 파라미터를 JSON으로 남겨 외부 요청 직전 값을 확인하게 추가함 -3/17
            Map<String, Object> revokeRequestPayload = new LinkedHashMap<>();
            revokeRequestPayload.put("grantType", "delete");
            revokeRequestPayload.put("clientId", naverClientId);
            revokeRequestPayload.put("accessToken", accessToken);
            revokeRequestPayload.put("serviceProvider", "NAVER");
            logJson("logout.revoke.request", revokeRequestPayload);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://nid.naver.com/oauth2.0/token", entity, String.class);

            // 네이버 토큰 해제 응답 상태와 본문을 JSON으로 남겨 외부 응답 확인을 쉽게 추가함 -3/17
            Map<String, Object> revokeResponsePayload = new LinkedHashMap<>();
            revokeResponsePayload.put("status", response.getStatusCode().value());
            revokeResponsePayload.put("body", response.getBody());
            logJson("logout.revoke.response", revokeResponsePayload);

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

//        // 3. JWT 사용 시 쿠키에서 JWT 토큰 제거
//        Cookie cookie = new Cookie("jwt_token", null);
//        cookie.setMaxAge(0);
//        cookie.setPath("/");
//        response.addCookie(cookie);

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
        // 로그인 사용자 모델 정보도 공통 인증 사용자 조회 결과를 재사용함
        Optional<User> currentUser = isLoggedIn ? userService.getAuthenticatedUser(authentication) : Optional.empty();
        model.addAttribute("currentUserId", currentUser.map(User::getId).orElse(null));

        if (!isLoggedIn) {
            return;
        }

        model.addAttribute("authName", authentication.getName());

        // 화면 표시 이름과 이메일은 provider별 response가 아니라 DB 사용자 기준으로 맞춤
        currentUser.ifPresentOrElse(user -> {
            model.addAttribute("provider", user.getProvider());
            model.addAttribute("displayName", user.getUserName() != null ? user.getUserName() : authentication.getName());
            model.addAttribute("email", user.getEmail());
        }, () -> model.addAttribute("displayName", authentication.getName()));
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        // 화면 currentUserId도 공통 인증 사용자 해석 결과의 내부 PK를 사용하게 맞춤
        return userService.getAuthenticatedUser(authentication)
                .map(User::getId)
                .orElse(null);
    }

    private String resolveProvider(Authentication authentication, Map<String, Object> attributes) {
        // OAuth2 provider 값을 인증 토큰에서 우선 읽고 없으면 정규화 attributes를 사용함
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getAuthorizedClientRegistrationId();
        }
        Object provider = attributes.get("provider");
        return provider instanceof String providerText ? providerText : "unknown";
    }

    private Object resolveOAuthAttribute(OAuth2User oauth2User, String key) {
        // provider별 응답 차이는 정규화 attributes를 우선 사용하고 네이버 response는 fallback으로만 처리함
        Map<String, Object> attributes = oauth2User.getAttributes();
        Object value = attributes.get(key);
        if (value != null) {
            return value;
        }
        Object responseObj = attributes.get("response");
        if (responseObj instanceof Map<?, ?> response) {
            return response.get(key);
        }
        return null;
    }

    // OAuth2 컨트롤러 단계별 객체를 JSON 문자열로 남겨 흐름 비교가 쉬워지게 추가함 -3/17
    private void logJson(String label, Object payload) {
        try {
            log.info("[OAuth2] {}={}", label, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[OAuth2] {}={}", label, payload);
        }
    }
}
