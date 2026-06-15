package com.project.marketplace.order.entity;

public enum OrderStatus {
    PENDING,   // 배송 대기 중
    PAID,      // 결제 완료
    SHIPPING,  // 배송 중
    COMPLETED, // 배송 완료
    CANCELED   // 주문 취소
}
