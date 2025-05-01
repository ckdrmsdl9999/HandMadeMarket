package com.project.marketplace.product.service;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + productId));
        return toDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> searchProductsByName(String keyword) {
        return productRepository.findByNameContaining(keyword)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createProduct(ProductDto dto) {
        Product product = toEntity(dto);
        if (product.getSalesCount() == null) product.setSalesCount(0);
        if (product.getQuantity() == null) product.setQuantity(0);
        product.setIsSoldOut(product.getQuantity() <= 0);
        return productRepository.save(product).getId();
    }

    @Transactional
    public void updateProduct(ProductDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        product.setName(dto.getProductName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setDescription(dto.getMainImage());
        product.setIsSoldOut(dto.getQuantity() <= 0);

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    @Transactional
    public void updateProductQuantity(Long productId, Integer quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + productId));

        if (quantityChange < 0 && product.getQuantity() < -quantityChange) {
            throw new RuntimeException("상품 재고가 부족합니다.");
        }

        product.setQuantity(product.getQuantity() + quantityChange);
        product.setIsSoldOut(product.getQuantity() <= 0);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getPopularProducts(int limit) {
        return productRepository.findPopularProducts(PageRequest.of(0, limit))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void purchaseProduct(Long productId, Integer quantity) {
        updateProductQuantity(productId, -quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        product.setSalesCount(product.getSalesCount() + quantity);
        productRepository.save(product);
    }

    // ------------------ DTO <-> Entity 변환 ------------------

    private ProductDto toDto(Product product) {
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

    private Product toEntity(ProductDto dto) {
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
