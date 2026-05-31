package com.project.marketplace.delivery.dto;

import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryUpdateResponseDto {

    private Long id;
    private Long orderId;
    private String address;
    private DeliveryStatus status;
    private LocalDateTime createdAt;

    public static DeliveryUpdateResponseDto fromEntity(Delivery delivery) {
        return DeliveryUpdateResponseDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrder().getOrderId())
                .address(delivery.getAddress())
                .status(delivery.getStatus())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}
