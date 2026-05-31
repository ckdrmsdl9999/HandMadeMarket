package com.project.marketplace.order.dto;

import com.project.marketplace.order.entity.Order;
import com.project.marketplace.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 주문 조회 응답은 주문 기본 정보와 주문 상품 목록을 함께 내려주도록 분리함
@Getter
@Builder
public class OrderResponseDto {

    private Long orderId;
    private Long userId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private Integer totalAmount;
    private LocalDateTime orderDate;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private List<OrderItemResponseDto> items;

    public static OrderResponseDto fromEntity(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .items(order.getOrderItems().stream()
                        .map(OrderItemResponseDto::fromEntity)
                        .toList())
                .build();
    }
}
