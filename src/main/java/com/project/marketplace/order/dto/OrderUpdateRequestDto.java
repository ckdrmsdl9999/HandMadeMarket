package com.project.marketplace.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 수정 요청은 사용자가 바꿀 수 있는 수령 정보만 받도록 분리함
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequestDto {

    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
}
