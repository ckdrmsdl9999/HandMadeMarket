package com.project.marketplace.user.controller;

import com.project.marketplace.user.dto.RoleUpdateDto;
import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.dto.UserSignInDto;
import com.project.marketplace.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody  UserDto userDto) {
        System.out.println(userDto);
        userService.insertUser(userDto);
        return ResponseEntity.status(HttpStatus.OK).body("계정생성 성공");
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody UserSignInDto signInDto, HttpSession session) {
        userService.checkoutUser(signInDto);
        System.out.print("hi~~");
        return ResponseEntity.status(HttpStatus.OK).body("로그인성공");
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        System.out.print("로그아웃 성공");
        return ResponseEntity.status(HttpStatus.OK).body("로그아웃성공");
    }

    // 사용자 ID로 조회
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        UserDto user = userService.getUserById(userId);
        if (user != null) {
            return ResponseEntity.status(HttpStatus.OK).body(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getUserList() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    // 사용자 정보 업데이트
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody UserDto userDto) {
        userDto.setUserId(userId);// URL의 ID와 일치시킴

        boolean updated = userService.updateUser(userDto);
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 정보 업데이트 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("업데이트할 사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 역할 업데이트
    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long userId, @RequestBody RoleUpdateDto roleUpdateDto) {
        boolean updated = userService.updateUserRole(userId, roleUpdateDto.getRole());
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 역할 업데이트 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("업데이트할 사용자를 찾을 수 없습니다");
        }
    }

    // 사용자 삭제
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK).body("사용자 삭제 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 사용자를 찾을 수 없습니다");
        }
    }
}


