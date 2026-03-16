package com.project.marketplace.order.dto;

import com.project.marketplace.order.entity.Order;

import com.project.marketplace.order.entity.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {//쿠폰 선착순 100명 이런식으로 추가고려

    private Long orderId;              // 주문 ID
    private Long userId;               // 주문자 ID
    private String orderNumber;        // 주문 번호
    private OrderStatus orderStatus;        // 주문 상태 (PENDING, PAID, SHIPPING, COMPLETED, CANCELED)
    private Integer totalAmount;       // 총 주문 금액
    private LocalDateTime orderDate;   // 주문 일시
    private String recipientName;      // 수령인 이름
    private String recipientPhone;     // 수령인 연락처
    private String shippingAddress;    // 배송지 주소


    public static OrderDto fromEntity(Order order) {
        return OrderDto.builder()
                .orderId(order.getOrderId())
                // 주문 DTO의 userId는 API가 사용하는 내부 PK를 유지하도록 User.id를 노출한다 -3/16
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .build();
    }

    public static Order toEntity(OrderDto dto) {
        // 사용자 연관관계는 서비스 계층에서 사용자 조회 검증 후 세팅하도록 분리해 무결성을 보장한다.
        return Order.builder()
                .orderId(dto.getOrderId())
                .orderNumber(dto.getOrderNumber())
                .orderStatus(dto.getOrderStatus())
                .totalAmount(dto.getTotalAmount())
                .orderDate(dto.getOrderDate())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .shippingAddress(dto.getShippingAddress())
                .build();
    }
}
