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
// 사용자 조회 응답 DTO
public class UserResponseDto {

    private Long id;
    private String loginId;
    private String userName;
    private UserRole role;
    private String email;
    private String provider;

    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .userName(user.getUserName())
                .role(user.getRole())
                .email(user.getEmail())
                .provider(user.getProvider())
                .build();
    }
}
