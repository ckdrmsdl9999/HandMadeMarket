package com.project.marketplace.order.entity;

import com.project.marketplace.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    // 주문자 FK를 단순 ID에서 User 연관객체로 전환해 주문-사용자 관계를 JPA로 일관되게 관리한다.
//    @Column(nullable = false)
//    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String shippingAddress;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 양방향 연관관계를 한 곳에서 관리해 order_items 저장 누락을 방지한다.
    public void addItem(OrderItem orderItem) {
        orderItem.setOrder(this);
        this.orderItems.add(orderItem);
    }
}
