package com.project.marketplace.user.controller;

import com.project.marketplace.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User,
                               Model model,
                               Authentication authentication) {

        // oauth2User가 null인 경우 처리 (직접 URL 접속 시)
//        if (oauth2User == null) {
//            System.out.println("oauth2User is null!");
//            return "redirect:/"; // 홈페이지로 리다이렉트
//        }
        System.out.println("oauth2User attributes: " + oauth2User.getAttributes());

        try {
            // OAuth2User에서 사용자 정보 추출
            Map<String, Object> attributes = oauth2User.getAttributes();
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            String email = (String) response.get("email");

            // JWT 토큰 생성
            String token = jwtTokenProvider.createToken(email,
                    (Collection<? extends GrantedAuthority>) authentication.getAuthorities());

            // 모델에 토큰 추가
            model.addAttribute("token", token);
            model.addAttribute("userName", response.get("name"));

            return "login-success"; // 뷰 이름 반환
        } catch (Exception e) {
            // 예외 처리
            return "redirect:/";
        }
    }

    // 홈 페이지
    @GetMapping("/")
    public String home() {
        return "home"; // home.html 템플릿 반환
    }
}