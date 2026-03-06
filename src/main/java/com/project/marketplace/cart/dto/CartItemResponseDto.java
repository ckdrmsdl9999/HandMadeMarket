package com.project.marketplace.cart.dto;

import com.project.marketplace.cart.entity.CartItem;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private Long cartItemId;
    private Long productId;
    private String productNameSnapshot;
    private Integer unitPriceSnapshot;
    private Integer quantity;
    private Integer lineAmount;


    public static CartItemResponseDto fromEntity(CartItem cartItem) {
        return CartItemResponseDto.builder()
                .cartItemId(cartItem.getCartItemId())
                .productId(cartItem.getProduct().getId())
                .productNameSnapshot(cartItem.getProductNameSnapshot())
                .unitPriceSnapshot(cartItem.getUnitPriceSnapshot())
                .quantity(cartItem.getQuantity())
                .lineAmount(cartItem.getLineAmount())
                .build();
    }
}
