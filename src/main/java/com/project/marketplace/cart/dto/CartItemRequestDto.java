package com.project.marketplace.cart.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {


    private Long productId;


    private Integer quantity;
}
