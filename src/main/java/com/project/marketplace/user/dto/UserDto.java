package com.project.marketplace.user.dto;

import com.project.marketplace.user.entity.User;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long userId;
    private String userName;
    private String password;
    private String role;
    private String email;
    private String provider;
    private String providerId;

    public User toEntity() {
        return User.builder()
                .userId(userId)
                .userName(userName)
                .password(password)
                .role(role)
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .password(user.getPassword())
                .role(user.getRole())
                .email(user.getEmail())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .build();
    }
}