package com.project.marketplace.product.entity;


import com.project.marketplace.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품명
    @Column(nullable = false, length = 100)
    private String name;

    // 카테테고리(String으로 )
    @Column(nullable = false, length = 50)
    private String category;

    //재고 수량
    @Column(nullable = false)
    private Integer quantity;

    // 판매수량
    @Column(nullable = false)
    private Integer salesCount;

    // 가격
    @Column(nullable = false)
    private Integer price;

    private String description;

    // 품절 여부
    @Column(nullable = false)
    private Boolean isSoldOut;

    // 상품은 반드시 판매자에 속하도록 DB와 JPA 양쪽에서 필수 관계로 제한함
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
}
