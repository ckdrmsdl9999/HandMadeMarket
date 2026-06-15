package com.project.marketplace.order.dto;

import com.project.marketplace.order.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

// 주문 조회 응답에서 주문 당시 상품 스냅샷과 수량을 내려주기 위한 DTO
@Getter
@Builder
public class OrderItemResponseDto {

    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer lineAmount;

    public static OrderItemResponseDto fromEntity(OrderItem item) {
        return OrderItemResponseDto.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .productName(item.getProductNameSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineAmount(item.getLineAmount())
                .build();
    }
}
