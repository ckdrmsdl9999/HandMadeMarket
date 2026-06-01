package com.project.marketplace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
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

// Swagger 문서에는 API가 아닌 화면 이동 컨트롤러를 노출하지 않도록 숨김
@Hidden
@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuthController {//일단 만들어보자구

    private final UserService userService;
    // OAuth2 화면 전환과 로그아웃 데이터를 JSON 로그로 같은 방식에 확인하게 맞춤 -3/17
    private final ObjectMapper objectMapper;

    @GetMapping("/loginSuccess")
    public String loginSuccess(Model model, Authentication authentication) {
        // 내 정보 화면은 OAuth2User가 아닌 DB 사용자 기준으로 구성해 로컬 로그인도 접근 가능하게 함
        Optional<User> currentUser = resolveCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        addAuthInfoToModel(model, authentication);
        User user = currentUser.get();

        model.addAttribute("userId", user.getId());
        model.addAttribute("loginId", user.getLoginId());
        model.addAttribute("userName", user.getUserName());
        model.addAttribute("provider", user.getProvider());
        // provider 원본값 대신 화면에서 이해하기 쉬운 회원 유형 라벨을 함께 내려줌
        model.addAttribute("providerLabel", resolveProviderLabel(user.getProvider()));
        model.addAttribute("providerId", user.getLoginId());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole().name());

        return "login-success";
    }

    @GetMapping("/oauth2/callback")
    public String loginSuccess3(Model model, Authentication authentication) {
        // callback 화면 처리도 DB 사용자 기준 내 정보 화면으로 통일함
        return loginSuccess(model, authentication);
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "login";
    }

    // 회원가입 버튼이 실제 화면으로 이동하도록 별도 회원가입 뷰를 연결함
    @GetMapping("/signup")
    public String signup(Model model, Authentication authentication) {
        addAuthInfoToModel(model, authentication);
        return "signup";
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
            // 공통 로그인 상태 영역에서도 provider 원본값 대신 회원 유형 라벨을 표시하게 함
            model.addAttribute("providerLabel", resolveProviderLabel(user.getProvider()));
            model.addAttribute("displayName", user.getUserName() != null ? user.getUserName() : authentication.getName());
            model.addAttribute("email", user.getEmail());
        }, () -> {
            // DB 사용자 조회가 실패해도 화면에는 원본 인증 이름 대신 기본 회원 유형을 표시함
            model.addAttribute("displayName", authentication.getName());
            model.addAttribute("providerLabel", resolveProviderLabel(null));
        });
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

    // provider 저장값을 사용자에게 보이는 회원 유형 이름으로 변환함
    private String resolveProviderLabel(String provider) {
        if ("naver".equalsIgnoreCase(provider)) {
            return "네이버 방식 회원";
        }
        if ("google".equalsIgnoreCase(provider)) {
            return "구글 방식 회원";
        }
        if ("local".equalsIgnoreCase(provider)) {
            return "일반 회원";
        }

        return "기타 회원";
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
