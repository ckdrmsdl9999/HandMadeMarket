package com.project.marketplace.product.service;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.storage.ImageStorageService;
import com.project.marketplace.storage.ImageUploadType;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.entity.UserRole;
import com.project.marketplace.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    @Test
    void createProductWithImageStoresUploadedImageUrl() {
        ProductRepository productRepository = mock(ProductRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProductService productService = new ProductService(productRepository, userRepository, imageStorageService);
        User seller = User.builder()
                .id(7L)
                .role(UserRole.SELLER)
                .userName("판매자")
                .provider("local")
                .build();
        ProductDto dto = ProductDto.builder()
                .productName("수제 컵")
                .category("주방")
                .price(12000)
                .quantity(3)
                .description("핸드메이드 컵")
                .sellerId(7L)
                .build();
        MockMultipartFile imageFile = new MockMultipartFile("mainImageFile", "cup.jpg", "image/jpeg", "image".getBytes());
        when(userRepository.findById(7L)).thenReturn(Optional.of(seller));
        when(imageStorageService.upload(imageFile, ImageUploadType.PRODUCT))
                .thenReturn("https://cdn.example.com/products/cup.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(10L);
            return product;
        });

        Long productId = productService.createProduct(dto, imageFile);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        // 이미지 파일이 있을 때 업로드 URL이 mainImage로 저장되는지 검증함
        assertThat(productCaptor.getValue().getMainImage()).isEqualTo("https://cdn.example.com/products/cup.jpg");
        assertThat(productCaptor.getValue().getDescription()).isEqualTo("핸드메이드 컵");
        assertThat(productId).isEqualTo(10L);
    }
}
