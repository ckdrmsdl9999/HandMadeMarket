package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.dto.UserSignInDto;
import com.project.marketplace.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;

    public void insertUser(UserDto user) {


        userMapper.insertUser(user);

    }

    public UserDto checkoutUser(UserSignInDto userSignInDto) {
        UserDto user = userMapper.findByUsername(userSignInDto.getUserName()).orElse(null);
        if (user == null || !user.getPassword().equals(userSignInDto.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다");
        }
        return user;
    }

    // ID로 사용자 조회
    public UserDto getUserById(Long userId) {
        return userMapper.findById(userId).orElse(null);
    }

    // 모든 사용자 목록 조회
    public List<UserDto> getAllUsers() {
        return userMapper.findAll();
    }

    // 사용자 정보 업데이트
    public boolean updateUser(UserDto userDto) {
        int result = userMapper.updateUser(userDto);
        return result > 0;
    }

    // 사용자 역할만 업데이트
    public boolean updateUserRole(Long userId, String role) {
        int result = userMapper.updateRole(userId, role);
        return result > 0;
    }

    // 사용자 삭제
    public boolean deleteUser(Long userId) {
        int result = userMapper.deleteUser(userId);
        return result > 0;
    }




}
