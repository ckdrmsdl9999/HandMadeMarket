package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.*;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void insertUser(UserSignUpDto userSignUpDto) {
        if (userSignUpDto.getLoginId() == null || userSignUpDto.getLoginId().isBlank()) {
            throw new RuntimeException("로그인 아이디는 필수입니다.");
        }

        if (userSignUpDto.getPassword() == null || userSignUpDto.getPassword().isBlank()) {
            throw new RuntimeException("비밀번호는 필수입니다.");
        }

        if (userSignUpDto.getUserName() == null || userSignUpDto.getUserName().isBlank()) {
            throw new RuntimeException("이름은 필수입니다.");
        }


        if (userRepository.findByLoginId(userSignUpDto.getLoginId()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        userSignUpDto.setRole(UserRole.USER);
        userSignUpDto.setProvider("local");
        userSignUpDto.setPassword(passwordEncoder.encode(userSignUpDto.getPassword()));

        userRepository.save(userSignUpDto.toEntity());
    }

    public UserDto checkoutUser(UserSignInDto userSignInDto) {

        String loginId = resolveLoginId(userSignInDto);

        User user =
                userRepository.findByLoginId(loginId).orElseThrow(()->new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다"));

        if (user.isDeleted()) {
            throw new RuntimeException("탈퇴 처리된 계정입니다");
        }

        if (user.getPassword() == null
                || userSignInDto.getPassword() == null
                || !passwordEncoder.matches(userSignInDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다");
        }


        return UserDto.fromEntity(user);
    }

    public UserResponseDto getUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .map(UserResponseDto::fromEntity)
                .orElse(null);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }


    public boolean updateUser(String loginId, UserUpdateDto userUpdateDto) {

        Optional<User> userOpt = userRepository.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (userUpdateDto.getUserName() != null) {
            user.setUserName(userUpdateDto.getUserName());
        }

        if (userUpdateDto.getEmail() != null) {
            user.setEmail(userUpdateDto.getEmail());
        }

        if (userUpdateDto.getPassword() != null && !userUpdateDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userUpdateDto.getPassword()));
        }

        userRepository.save(user);
        return true;
    }

    public boolean updateUserRole(Long userId, UserRole role) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setRole(role);
        userRepository.save(user);
        return true;
    }


    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) return false;
        userRepository.deleteById(userId);
        return true;
    }

    public void requestDeletion(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }


    private String resolveLoginId(UserSignInDto userSignInDto) {
        if (userSignInDto.getLoginId() != null && !userSignInDto.getLoginId().isBlank()) {
            return userSignInDto.getLoginId();
        }
//        if (userSignInDto.getUserName() != null && !userSignInDto.getUserName().isBlank()) {
//            return userSignInDto.getUserName();
//        }
        throw new RuntimeException("로그인 아이디는 필수입니다");
    }
}
