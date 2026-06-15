package com.project.marketplace.delivery.entity;

// 배송 상태를 정해진 값으로만 저장해 잘못된 문자열 저장을 막음
public enum DeliveryStatus {
    READY,
    SHIPPING,
    DELIVERED,
    CANCELED
}
