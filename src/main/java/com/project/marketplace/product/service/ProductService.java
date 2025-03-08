package com.project.marketplace.product.service;

import com.project.marketplace.product.dto.ProductDto;
import com.project.marketplace.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    /**
     * 모든 상품 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productMapper.findAll();
    }

    /**
     * 상품 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        return productMapper.findById(productId);
    }

    /**
     * 카테고리별 상품 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productMapper.findByCategory(category);
    }

    /**
     * 상품명으로 상품을 검색합니다.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> searchProductsByName(String keyword) {
        return productMapper.searchByName(keyword);
    }

    /**
     * 새로운 상품을 등록합니다.
     */
    @Transactional
    public Long createProduct(ProductDto productDto) {
        // 기본값 설정
        if (productDto.getIsSoldOut() == null) {
            productDto.setIsSoldOut(productDto.getQuantity() <= 0);
        }
        if (productDto.getSalesCount() == null) {
            productDto.setSalesCount(0);
        }

        productMapper.insertProduct(productDto);
        return productDto.getProductId();
    }

    /**
     * 상품 정보를 수정합니다.
     */
    @Transactional
    public void updateProduct(ProductDto productDto) {
        // 품절 상태 확인
        if (productDto.getQuantity() <= 0) {
            productDto.setIsSoldOut(true);
        }

        productMapper.updateProduct(productDto);
    }

    /**
     * 상품을 삭제합니다.
     */
    @Transactional
    public void deleteProduct(Long productId) {
        productMapper.deleteProduct(productId);
    }

    /**
     * 상품 재고를 업데이트합니다.
     * @param productId 상품 ID
     * @param quantity 변경할 수량 (감소: 음수, 증가: 양수)
     */
    @Transactional
    public void updateProductQuantity(Long productId, Integer quantity) {
        ProductDto product = productMapper.findById(productId);
        if (product == null) {
            throw new RuntimeException("상품을 찾을 수 없습니다. ID: " + productId);
        }

        // 재고 감소 시 충분한 재고가 있는지 확인
        if (quantity < 0 && product.getQuantity() < Math.abs(quantity)) {
            throw new RuntimeException("상품 재고가 부족합니다.");
        }

        productMapper.updateQuantity(productId, quantity);
    }

    /**
     * 인기 상품 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getPopularProducts(int limit) {
        return productMapper.findPopularProducts(limit);
    }

    /**
     * 상품 구매 처리 (재고 감소 및 판매 수량 증가)
     */
    @Transactional
    public void purchaseProduct(Long productId, Integer quantity) {
        // 재고 감소
        updateProductQuantity(productId, -quantity);

        // 판매 수량 증가
        productMapper.updateSalesCount(productId, quantity);
    }

}
