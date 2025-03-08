package com.project.marketplace.product.dto;


import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productId;        // 상품 ID
    private String productName;    // 상품명
    private String category;       // 카테고리
    private Integer price;         // 가격
    private Boolean isSoldOut;     // 품절 여부
    private Integer quantity;      // 재고
    private Integer salesCount;    // 판매 수량
    private String mainImage;    // 메인 이미지 경로

}
