package com.project.marketplace.user.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateDto {

    private String role;

    // Getter와 Setter
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
