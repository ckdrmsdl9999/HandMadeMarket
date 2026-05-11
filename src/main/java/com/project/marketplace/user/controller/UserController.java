package com.project.marketplace.user.controller;

import com.project.marketplace.user.dto.*;
import com.project.marketplace.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;//주석

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignUpDto userSignUpDto) {
        userService.insertUser(userSignUpDto);
        return ResponseEntity.status(HttpStatus.OK).body("계정생성 성공");
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody UserSignInDto signInDto, HttpSession session) {
        UserDto userDto = userService.checkoutUser(signInDto);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(signInDto.getLoginId(),
                        null,List.of(new SimpleGrantedAuthority("ROLE_"+userDto.getRole())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);


        return ResponseEntity.status(HttpStatus.OK).body("로그인성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.status(HttpStatus.OK).body("로그아웃성공");
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> requestDeletion(Authentication authentication, HttpSession session) {
        userService.requestDeletion(authentication.getName());
        session.invalidate();
        return ResponseEntity.ok("탈퇴 신청 완료");
    }

    // 사용자 ID로 조회
    @GetMapping("/me")
    public ResponseEntity<?> getUserById(Authentication authentication) {
        UserResponseDto user = userService.getUser(authentication.getName());
        if (user != null) {
            return ResponseEntity.status(HttpStatus.OK).body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getUserList() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    // 사용자 정보 업데이트
    @PutMapping("/me")
    public ResponseEntity<?> updateUser(Authentication authentication, @RequestBody UserUpdateDto userUpdateDto) {

        boolean updated = userService.updateUser(authentication.getName(), userUpdateDto);
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 정보 업데이트 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("업데이트할 사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 역할 업데이트
    @PutMapping("/admin/users/{userId}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long userId, @RequestBody RoleUpdateDto roleUpdateDto) {
        boolean updated = userService.updateUserRole(userId, roleUpdateDto.getRole());
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 역할 업데이트 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("업데이트할 사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 삭제
    @DeleteMapping("/admin/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 삭제 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 사용자를 찾을 수 없습니다");
        }
    }


}


