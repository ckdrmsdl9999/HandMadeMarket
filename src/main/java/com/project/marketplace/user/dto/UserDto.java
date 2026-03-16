package com.project.marketplace.user.dto;

import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {


    private Long id;
    private String loginId;
    private String userName;
    private String password;
    private UserRole role;
    private String email;
    private String provider;

    public User toEntity() {
        return User.builder()
                .id(id)
                .loginId(loginId)
                .userName(userName)
                .password(password)
                .role(role)
                .email(email)
                .provider(provider)
                .build();
    }

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .userName(user.getUserName())
                .password(user.getPassword())
                .role(user.getRole())
                .email(user.getEmail())
                .provider(user.getProvider())
                .build();
    }
}
