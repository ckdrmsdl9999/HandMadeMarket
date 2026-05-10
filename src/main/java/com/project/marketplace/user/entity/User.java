package com.project.marketplace.user.entity;
import com.project.marketplace.order.entity.Order;
import com.project.marketplace.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;


@Entity@Table(name = "users") // 실제 DB 테이블명에 맞게 수정
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;//db내부식별

    @Column(nullable = false, unique = true)
    private String loginId;//실제 로그인할때 입력하는 id값

//    @Column(nullable = false)
    @Column
    private String password;

    @Enumerated(EnumType.STRING)//"USER", "SELLER", "ADMIN"
    @Column(nullable = false, length = 20)
    private UserRole role;

    // OAuth2 관련 필드 추가
    @Column
    private String email;

    @Column
    private String provider; // local, naver

    @Column
    private String userName; // 사용자의 이름

    @Column
    private String accessToken;

    @Column
    private LocalDateTime tokenExpiresAt;

    // 판매자가 등록한 상품을 사용자 기준으로 조회할 수 있게 하려고 상품 컬렉션 연관을 추가했다.
    @Builder.Default
    @OneToMany(mappedBy = "seller")
    private List<Product> products = new ArrayList<>();

    // 사용자 기준으로 주문 목록을 조회할 수 있도록 주문 컬렉션 연관관계를 추가했다.
    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Order> orders = new ArrayList<>();

}
