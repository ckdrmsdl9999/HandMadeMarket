package com.project.marketplace.user.dto;

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
// 회원수정 DTO
public class UserUpdateDto {


    private String userName;
    private String password;
    private String email;

}
