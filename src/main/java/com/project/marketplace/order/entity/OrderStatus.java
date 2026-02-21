package com.project.marketplace.order.entity;

public enum OrderStatus {
    PENDING,   // 주문 생성
    PAID,      // 결제 완료
    SHIPPING,  // 배송 중
    COMPLETED, // 구매 확정
    CANCELED   // 주문 취소
}
