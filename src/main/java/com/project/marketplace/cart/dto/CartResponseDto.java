package com.project.marketplace.cart.dto;

import com.project.marketplace.cart.entity.Cart;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {

    private Long cartId;
    private Long userId;
    private Integer totalAmount;
    private List<CartItemResponseDto> cartItems;


    public static CartResponseDto fromEntity(Cart cart) {
        List<CartItemResponseDto> itemDtos = cart.getCartItems().stream()
                .map(CartItemResponseDto::fromEntity)
                .toList();

        int totalAmount = itemDtos.stream()
                .mapToInt(CartItemResponseDto::getLineAmount)
                .sum();

        return CartResponseDto.builder()
                .cartId(cart.getCartId())
                // 장바구니 응답의 userId는 프론트가 API 경로에 재사용하는 내부 PK로 통일했다 -3/16
                .userId(cart.getUser().getId())
                .totalAmount(totalAmount)
                .cartItems(itemDtos)
                .build();
    }


    public static CartResponseDto empty(Long userId) {
        return CartResponseDto.builder()
                .cartId(null)
                .userId(userId)
                .totalAmount(0)
                .cartItems(List.of())
                .build();
    }
}
