package com.project.marketplace.user.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "users") // 실제 DB 테이블명에 맞게 수정
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String userName;

//    @Column(nullable = false)
    @Column
    private String password;

    @Column(nullable = false)
    private String role; // (USER, ADMIN)

    // OAuth2 관련 필드 추가
    @Column
    private String email;

    @Column
    private String provider; // local, naver

    @Column
    private String providerId; // OAuth 제공자의 ID

    @Column
    private String accessToken;

    @Column
    private LocalDateTime tokenExpiresAt;

}
