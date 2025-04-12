package com.project.marketplace.user.controller;

import com.project.marketplace.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {

        // OAuth2User에서 사용자 정보 추출
        Map<String, Object> attributes = oauth2User.getAttributes();
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        String email = (String) response.get("email");

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(email,
                (Collection<? extends GrantedAuthority>) authentication.getAuthorities());

        // 리다이렉트할 URL에 토큰 추가
        redirectAttributes.addAttribute("token", token);

        return "redirect:/"; // 홈페이지로 리다이렉트
    }
}