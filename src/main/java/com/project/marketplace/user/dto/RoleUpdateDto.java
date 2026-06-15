package com.project.marketplace.user.dto;


import com.project.marketplace.user.entity.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateDto {

    private UserRole role;

    // Getter와 Setter
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
