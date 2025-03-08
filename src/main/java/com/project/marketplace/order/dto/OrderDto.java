package com.project.marketplace.order.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long orderId;              // 주문 ID
    private Long userId;               // 주문자 ID
    private String orderNumber;        // 주문 번호
    private String orderStatus;        // 주문 상태 (PENDING, PAID, SHIPPING, COMPLETED, CANCELED)
    private Integer totalAmount;       // 총 주문 금액
    private LocalDateTime orderDate;   // 주문 일시
    private String recipientName;      // 수령인 이름
    private String recipientPhone;     // 수령인 연락처
    private String shippingAddress;    // 배송지 주소
    private String paymentMethod;      // 결제 방법


}
