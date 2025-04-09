package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.dto.UserSignInDto;
import com.project.marketplace.user.entity.User;
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
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userRepository.save(userDto.toEntity());
    }

    public UserDto checkoutUser(UserSignInDto userSignInDto) {
        Optional<User> userOpt = userRepository.findByUserName(userSignInDto.getUserName());

        if (userOpt.isEmpty() || !passwordEncoder.matches(userSignInDto.getPassword(), userOpt.get().getPassword())) {
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
        if (!userRepository.existsById(userDto.getUserId())) {
            return false;
        }
        userRepository.save(userDto.toEntity());
        return true;
    }

    public boolean updateUserRole(Long userId, String role) {
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
}
