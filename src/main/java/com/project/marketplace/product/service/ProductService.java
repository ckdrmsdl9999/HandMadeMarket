package com.project.marketplace.product.service;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        // Order 서비스와 조회 실패 처리 규칙을 맞추기 위해 예외 대신 null을 반환하도록 변경했다.
        return productRepository.findById(productId)
                .map(ProductDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> searchProductsByName(String keyword) {
        return productRepository.findByNameContaining(keyword)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 판매자 관리 화면에서 현재 사용자 상품조회
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsBySellerId(Long sellerId) {
        return productRepository.findProductsBySellerId(sellerId)
                .stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createProduct(ProductDto dto) {
        // 상품 등록 시 sellerId로 사용자 엔티티를 연결해 상품-판매자 FK가 누락되지 않도록 했다.
        if (dto.getSellerId() == null) {
            throw new RuntimeException("판매자 ID는 필수입니다.");
        }
        // sellerId를 실제 사용자 엔티티로 조회해 연관관계 무결성을 보장한다.
        User seller = userRepository.findById(dto.getSellerId())
                .orElseThrow(() -> new RuntimeException("판매자를 찾을 수 없습니다. ID: " + dto.getSellerId()));

        Product product = ProductDto.toEntity(dto);
        if (product.getSalesCount() == null) product.setSalesCount(0);
        if (product.getQuantity() == null) product.setQuantity(0);
        product.setIsSoldOut(product.getQuantity() <= 0);
        // 생성 시점에 판매자 연관관계를 설정해 추후 사용자 기준 상품 조회가 가능하도록 했다.
        product.setSeller(seller);
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
        // 수정 요청에 sellerId가 포함되면 판매자 연관관계도 함께 갱신할 수 있게 처리했다.
        if (dto.getSellerId() != null) {
            // 전달된 sellerId를 검증해 존재하는 사용자로만 판매자 변경이 되도록 했다.
            User seller = userRepository.findById(dto.getSellerId())
                    .orElseThrow(() -> new RuntimeException("판매자를 찾을 수 없습니다. ID: " + dto.getSellerId()));
            product.setSeller(seller);
        }

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
                .map(ProductDto::fromEntity)
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
}
