package com.project.marketplace.user.repository;

import com.project.marketplace.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndLoginId(String provider, String loginId);

    Optional<User> findByAccessToken(String accessToken);


}
