package com.project.marketplace.cart.dto;

import com.project.marketplace.cart.entity.CartItem;
import com.project.marketplace.user.entity.User;
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
    // 주문서에서 서로 다른 판매자 상품 여부를 판단하려고 판매자 ID를 함께 내려줌
    private Long sellerId;
    private String productNameSnapshot;
    // 장바구니와 주문서 상품 카드에서 판매자명을 함께 보여주기 위해 응답에 추가함
    private String sellerName;
    private Integer unitPriceSnapshot;
    private Integer quantity;
    private Integer lineAmount;


    public static CartItemResponseDto fromEntity(CartItem cartItem) {
        return CartItemResponseDto.builder()
                .cartItemId(cartItem.getCartItemId())
                .productId(cartItem.getProduct().getId())
                // 판매자 ID를 응답에 담아 프론트가 이름 비교 없이 같은 판매자인지 판단하게 함
                .sellerId(resolveSellerId(cartItem))
                .productNameSnapshot(cartItem.getProductNameSnapshot())
                // 상품의 현재 판매자 표시명을 응답에 담아 프론트에서 별도 상품 조회 없이 표시함
                .sellerName(resolveSellerName(cartItem))
                .unitPriceSnapshot(cartItem.getUnitPriceSnapshot())
                .quantity(cartItem.getQuantity())
                .lineAmount(cartItem.getLineAmount())
                .build();
    }

    // 판매자 이름이 비어 있으면 로그인 ID로 대체해 화면 표시가 비지 않게 함
    private static String resolveSellerName(CartItem cartItem) {
        User seller = cartItem.getProduct().getSeller();
        if (seller == null) {
            return null;
        }
        if (seller.getUserName() != null && !seller.getUserName().isBlank()) {
            return seller.getUserName();
        }
        return seller.getLoginId();
    }

    // 판매자 정보가 없는 예외 데이터도 DTO 변환에서 오류가 나지 않게 처리함
    private static Long resolveSellerId(CartItem cartItem) {
        User seller = cartItem.getProduct().getSeller();
        return seller != null ? seller.getId() : null;
    }
}
