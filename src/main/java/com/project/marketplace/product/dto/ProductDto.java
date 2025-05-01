package com.project.marketplace.product.dto;

import com.project.marketplace.product.entity.Product;
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
    private String mainImage;      // 메인 이미지 경로


    public static ProductDto fromEntity(Product product) {
        return ProductDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .isSoldOut(product.getIsSoldOut())
                .quantity(product.getQuantity())
                .salesCount(product.getSalesCount())
                .mainImage(product.getDescription())
                .build();
    }

    public static Product toEntity(ProductDto dto) {
        return Product.builder()
                .id(dto.getProductId())
                .name(dto.getProductName())
                .category(dto.getCategory())
                .price(dto.getPrice())
                .isSoldOut(dto.getIsSoldOut())
                .quantity(dto.getQuantity())
                .salesCount(dto.getSalesCount())
                .description(dto.getMainImage())
                .build();
    }
}
