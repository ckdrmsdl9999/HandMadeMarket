package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.dto.UserSignInDto;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void insertUser(UserDto userDto) {
        if (userDto.getRole() == null) {
            userDto.setRole(UserRole.USER);
        }

        if (userDto.getProvider() == null || userDto.getProvider().isBlank()) {
            userDto.setProvider("local");
        }
        if ((userDto.getUserName() == null || userDto.getUserName().isBlank()) && userDto.getLoginId() != null) {
            userDto.setUserName(userDto.getLoginId());
        }

        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        userRepository.save(userDto.toEntity());
    }

    public UserDto checkoutUser(UserSignInDto userSignInDto) {

        String loginId = resolveLoginId(userSignInDto);
        Optional<User> userOpt = userRepository.findByLoginId(loginId)
                .or(() -> userRepository.findByUserName(loginId));

        if (userOpt.isEmpty()
                || userOpt.get().getPassword() == null
                || !passwordEncoder.matches(userSignInDto.getPassword(), userOpt.get().getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다");
        }

        return UserDto.fromEntity(userOpt.get());
    }

    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::fromEntity)
                .orElse(null);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean updateUser(UserDto userDto) {

        if (userDto.getId() == null) {
            return false;
        }
        Optional<User> userOpt = userRepository.findById(userDto.getId());
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (userDto.getLoginId() != null && !userDto.getLoginId().isBlank()) {
            user.setLoginId(userDto.getLoginId());
        }
        if (userDto.getUserName() != null) {
            user.setUserName(userDto.getUserName());
        }
        if (userDto.getRole() != null) {
            user.setRole(userDto.getRole());
        }
        if (userDto.getEmail() != null) {
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getProvider() != null && !userDto.getProvider().isBlank()) {
            user.setProvider(userDto.getProvider());
        }
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
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


    private String resolveLoginId(UserSignInDto userSignInDto) {
        if (userSignInDto.getLoginId() != null && !userSignInDto.getLoginId().isBlank()) {
            return userSignInDto.getLoginId();
        }
        if (userSignInDto.getUserName() != null && !userSignInDto.getUserName().isBlank()) {
            return userSignInDto.getUserName();
        }
        throw new RuntimeException("로그인 아이디는 필수입니다");
    }
}
