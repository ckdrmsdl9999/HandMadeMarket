package com.project.marketplace.delivery.dto;

import com.project.marketplace.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryUpdateRequestDto {
    private Long orderId;
    private String address;
    private DeliveryStatus status;
}
