package com.project.marketplace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {//일단 만들어보자구

    private final UserService userService;
    // OAuth2 화면 전환과 로그아웃 데이터를 JSON 로그로 같은 방식에 확인하게 맞춤 -3/17
    private final ObjectMapper objectMapper;

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
            // 로그인 성공 화면으로 넘어온 원본 attributes를 JSON으로 남겨 successHandler 다음 값을 바로 확인하게 맞춤
            logJson("controller.loginSuccess.attributes", attributes);

            model.addAttribute("userName", name);
            model.addAttribute("email", email);
            model.addAttribute("mobile", mobile);
            model.addAttribute("provider", provider);
            model.addAttribute("providerId", providerId);

            // 화면에 전달한 최종 모델 값을 JSON으로 남겨 템플릿에서 보는 값과 대조하기 쉽게 맞춤
            Map<String, Object> modelPayload = new LinkedHashMap<>();
            modelPayload.put("userName", name);
            modelPayload.put("email", email);
            modelPayload.put("mobile", mobile);
            modelPayload.put("provider", provider);
            modelPayload.put("providerId", providerId);
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

    // 전체 상품 탐색 화면을 API 상품 목록과 연결함
    @GetMapping("/shop")
    public String shop(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "shop";
    }

    // 주문서와 내 주문 목록 화면은 로그인 사용자 기준 API를 사용하므로 인증 후 접근하게 함
    @GetMapping("/orders")
    public String orders(Model model, Authentication authentication) {
        if (resolveCurrentUser(authentication).isEmpty()) {
            return "redirect:/login";
        }
        addAuthInfoToModel(model, authentication);
        return "orders";
    }

    // 관리자 사용자 관리 화면은 역할 변경/삭제 API를 다루므로 관리자만 진입하게 함
    @GetMapping("/admin/users")
    public String adminUsers(Model model, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/";
        }
        addAuthInfoToModel(model, authentication);
        return "admin-users";
    }

    // 관리자 배송 관리 화면은 전체 배송 목록과 삭제 API를 다루므로 관리자만 진입하게 함
    @GetMapping("/admin/delivery")
    public String adminDelivery(Model model, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/";
        }
        addAuthInfoToModel(model, authentication);
        return "admin-delivery";
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
        // OAuth2 로그아웃은 provider 토큰 폐기 없이 우리 서비스 세션만 정리함
        clearLocalAuthentication(request, response);
        return "redirect:/?logout=success";
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

    }

    private void addAuthInfoToModel(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getPrincipal());

        model.addAttribute("isLoggedIn", isLoggedIn);
        // 로그인 사용자 모델 정보도 공통 인증 사용자 조회 결과를 재사용함
        Optional<User> currentUser = isLoggedIn ? resolveCurrentUser(authentication) : Optional.empty();
        model.addAttribute("currentUserId", currentUser.map(User::getId).orElse(null));
        model.addAttribute("currentUserRole", currentUser.map(user -> user.getRole().name()).orElse(null));

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

    // 화면 라우트 권한 판단도 공통 인증 사용자 해석 결과를 재사용함
    private Optional<User> resolveCurrentUser(Authentication authentication) {
        return userService.getAuthenticatedUser(authentication);
    }

    // 관리자 전용 템플릿 진입 여부를 DB 사용자 역할 기준으로 판단함
    private boolean isAdmin(Authentication authentication) {
        return resolveCurrentUser(authentication)
                .map(user -> "ADMIN".equals(user.getRole().name()))
                .orElse(false);
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
