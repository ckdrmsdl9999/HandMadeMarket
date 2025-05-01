package com.project.marketplace.product.repository;


import com.project.marketplace.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상품 카테고리별 조회
    List<Product> findByCategory(String category);

    // 상품명으로 검색 (이름에 키워드 포함된 상품)
    List<Product> findByNameContaining(String keyword);


    @Query("SELECT p FROM Product p ORDER BY p.salesCount DESC")
    List<Product> findPopularProducts(Pageable pageable);
}
