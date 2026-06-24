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
    // 상품 상세 설명을 이미지 경로와 분리해 텍스트 설명으로 내려줌
    private String description;     // 상품 설명
    private String mainImage;      // 메인 이미지 경로
    // 상품 응답에서 판매자 식별값과 이름을 함께 내려주기 위해 판매자 필드를 추가했다.
    private Long sellerId;         // 판매자 ID
    private String sellerName;     // 판매자 이름


    public static ProductDto fromEntity(Product product) {
        return ProductDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .isSoldOut(product.getIsSoldOut())
                .quantity(product.getQuantity())
                .salesCount(product.getSalesCount())
                // 상품 설명과 이미지 URL을 각각 다른 응답 필드로 내려주게 수정함
                .description(product.getDescription())
                .mainImage(product.getMainImage())
                // 상품 응답의 판매자 식별값을 내부 PK로 맞춰 다른 도메인과 같은 사용자 키를 쓰게 수정함
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getUserName() : null)
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
                // 상품 설명과 이미지 URL을 엔티티의 분리된 컬럼에 각각 저장함
                .description(dto.getDescription())
                .mainImage(dto.getMainImage())
                .build();
    }
}
