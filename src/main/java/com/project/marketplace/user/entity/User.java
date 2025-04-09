package com.project.marketplace.user.entity;
import jakarta.persistence.*;
import lombok.*;


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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // (USER, ADMIN)



}
