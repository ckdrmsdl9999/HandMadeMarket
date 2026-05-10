package com.project.marketplace.user.dto;

import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 회원가입 DTO
public class UserSignUpDto {

    private String loginId;
    private String userName;
    private String password;
    private UserRole role;
    private String email;
    private String provider;

    public User toEntity() {
        return User.builder()
                .loginId(loginId)
                .userName(userName)
                .password(password)
                .role(role)
                .email(email)
                .provider(provider)
                .build();
    }
}
