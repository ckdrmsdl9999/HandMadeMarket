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
                .userId(order.getUserId())
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
        return Order.builder()
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
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
