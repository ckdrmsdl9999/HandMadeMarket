package com.project.marketplace.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lineAmount;

    // 주문 시점의 상품명/가격 스냅샷을 저장해 이후 상품 정보가 바뀌어도 주문 이력을 보존한다.
    public static OrderItem create(Long productId, String productNameSnapshot, Integer unitPrice, Integer quantity) {
        return OrderItem.builder()
                .productId(productId)
                .productNameSnapshot(productNameSnapshot)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .lineAmount(unitPrice * quantity)
                .build();
    }
}