package com.project.marketplace.user.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserSignInDto {

    private String userName;

    private String password;
}
