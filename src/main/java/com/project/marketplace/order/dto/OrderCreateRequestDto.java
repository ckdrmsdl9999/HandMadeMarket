package com.project.marketplace.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 생성 요청에서 클라이언트가 직접 입력해야 하는 수령 정보만 받음
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
}
