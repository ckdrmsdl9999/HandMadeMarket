package com.project.marketplace.product.dto;

import com.project.marketplace.product.entity.Product;
import com.project.marketplace.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDtoTest {

    @Test
    void fromEntitySeparatesDescriptionAndMainImage() {
        Product product = Product.builder()
                .id(1L)
                .name("수제 머그컵")
                .category("주방")
                .price(12000)
                .isSoldOut(false)
                .quantity(5)
                .salesCount(2)
                // 상품 설명과 이미지 URL이 응답에서 서로 덮어쓰지 않도록 분리 매핑을 검증함
                .description("손으로 빚은 도자기 머그컵")
                .mainImage("https://cdn.example.com/products/1/main.jpg")
                .seller(User.builder()
                        .id(3L)
                        .userName("도자기 공방")
                        .build())
                .build();

        ProductDto dto = ProductDto.fromEntity(product);

        assertThat(dto.getDescription()).isEqualTo("손으로 빚은 도자기 머그컵");
        assertThat(dto.getMainImage()).isEqualTo("https://cdn.example.com/products/1/main.jpg");
        assertThat(dto.getSellerId()).isEqualTo(3L);
        assertThat(dto.getSellerName()).isEqualTo("도자기 공방");
    }

    @Test
    void toEntitySeparatesDescriptionAndMainImage() {
        ProductDto dto = ProductDto.builder()
                .productId(1L)
                .productName("수제 머그컵")
                .category("주방")
                .price(12000)
                .isSoldOut(false)
                .quantity(5)
                .salesCount(2)
                // 상품 저장 시 설명과 이미지 URL이 분리된 엔티티 필드로 들어가는지 검증함
                .description("손으로 빚은 도자기 머그컵")
                .mainImage("https://cdn.example.com/products/1/main.jpg")
                .build();

        Product product = ProductDto.toEntity(dto);

        assertThat(product.getDescription()).isEqualTo("손으로 빚은 도자기 머그컵");
        assertThat(product.getMainImage()).isEqualTo("https://cdn.example.com/products/1/main.jpg");
    }
}
