package com.project.marketplace.user.dto;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignInDto {

    private String loginId;
//    private String userName;

    private String password;
}


