package com.project.marketplace.user.dto;

import com.project.marketplace.user.entity.User;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserDto {

    private Long userId;

    private String userName;

    private String password;

    private String role;


    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .password(user.getPassword())
                .role(user.getRole())
                .build();
    }

    public User toEntity() {
        return User.builder()
                .userId(this.userId)
                .userName(this.userName)
                .password(this.password)
                .role(this.role != null ? this.role : "USER")
                .build();
    }


}
