package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.*;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    // 로컬 계정 조회 기준을 provider/loginId 복합 유니크와 맞추기 위해 상수화
    private static final String LOCAL_PROVIDER = "local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
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

        // 로컬 회원 중복 검사를 provider/loginId 복합 유니크 기준으로 맞춤
        if (userRepository.findByProviderAndLoginId(LOCAL_PROVIDER, userSignUpDto.getLoginId()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        userSignUpDto.setRole(UserRole.USER);
        userSignUpDto.setProvider("local");
        userSignUpDto.setPassword(passwordEncoder.encode(userSignUpDto.getPassword()));

        userRepository.save(userSignUpDto.toEntity());
    }

    @Transactional
    public UserDto checkoutUser(UserSignInDto userSignInDto) {

        String loginId = resolveLoginId(userSignInDto);

//        User user =
//                userRepository.findByLoginId(loginId).orElseThrow(()->new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다"));

        // 로컬 로그인은 provider=local 범위에서 loginId를 조회함
        User user = userRepository.findByProviderAndLoginId(LOCAL_PROVIDER, loginId)
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다"));

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

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long userId) {
        // 내 정보 조회를 loginId 단독 조회가 아닌 내부 PK 기준으로 수정
        return userRepository.findById(userId)
                .map(UserResponseDto::fromEntity)
                .orElse(null);
    }

    @Transactional
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean updateUser(Long userId, UserUpdateDto userUpdateDto) {
        // 사용자 정보 수정 대상을 loginId가 아닌 내부 PK로 찾아 OAuth2 계정도 동일하게 처리
        Optional<User> userOpt = userRepository.findById(userId);
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

    @Transactional
    public boolean updateUserRole(Long userId, UserRole role) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setRole(role);
        userRepository.save(user);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<User> getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2User oauth2User = oauthToken.getPrincipal();

            if ("naver".equals(provider)) {
                Object responseObj = oauth2User.getAttributes().get("response");
                if (responseObj instanceof Map<?, ?> response) {
                    Object providerId = response.get("id");
                    if (providerId instanceof String providerIdText && !providerIdText.isBlank()) {
                        return userRepository.findByProviderAndLoginId(provider, providerIdText);
                    }
                }
                return Optional.empty();
            }

            if ("google".equals(provider)) {
                Object providerId = oauth2User.getAttributes().get("sub");
                if (providerId instanceof String providerIdText && !providerIdText.isBlank()) {
                    return userRepository.findByProviderAndLoginId(provider, providerIdText);
                }
                return Optional.empty();
            }

            return Optional.empty();
        }

        return userRepository.findByProviderAndLoginId(LOCAL_PROVIDER, authentication.getName());
    }


    @Transactional
    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) return false;
        userRepository.deleteById(userId);
        return true;
    }

    @Transactional
    public void requestDeletion(Long userId) {
        // 탈퇴 대상을 내부 PK로 찾아 provider별 loginId 중복 영향을 제거
        User user = userRepository.findById(userId)
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
