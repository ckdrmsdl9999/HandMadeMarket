package com.project.marketplace.product.mapper;

import com.project.marketplace.product.dto.ProductDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {


    /**
     * 새 상품을 등록합니다.
     */
    int insertProduct(ProductDto productDto);

    /**
     * 상품 ID로 상품을 조회합니다.
     */
    ProductDto findById(Long productId);

    /**
     * 모든 상품 목록을 조회합니다.
     */
    List<ProductDto> findAll();

    /**
     * 카테고리별 상품 목록을 조회합니다.
     */
    List<ProductDto> findByCategory(String category);

    /**
     * 상품명으로 상품을 검색합니다.
     */
    List<ProductDto> searchByName(String keyword);

    /**
     * 상품 정보를 수정합니다.
     */
    int updateProduct(ProductDto productDto);

    /**
     * 상품 재고를 업데이트합니다.
     */
    int updateQuantity(Long productId, Integer quantity);

    /**
     * 상품 판매 수량을 업데이트합니다.
     */
    int updateSalesCount(Long productId, Integer quantity);

    /**
     * 상품을 삭제합니다.
     */
    int deleteProduct(Long productId);

    /**
     * 인기 상품 목록을 조회합니다. (판매 수량 기준)
     */
    List<ProductDto> findPopularProducts(int limit);
}
