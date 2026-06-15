package com.project.marketplace.product.repository;


import com.project.marketplace.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상품 목록 응답에서 판매자 이름을 항상 사용하므로 상품과 판매자를 함께 조회해 N+1 추가 쿼리를 막음
    @Query("SELECT p FROM Product p JOIN FETCH p.seller ORDER BY p.id DESC")
    List<Product> findAllWithSeller();

    // 상품 카테고리별 조회
    List<Product> findByCategory(String category);

    // 상품명으로 검색 (이름에 키워드 포함된 상품)
    List<Product> findByNameContaining(String keyword);

    boolean existsByNameAndSeller_Id(String name, Long sellerId);

    @Query("SELECT p FROM Product p ORDER BY p.salesCount DESC")
    List<Product> findPopularProducts(Pageable pageable);

    // 판매자 페이지에서 내 상품 목록을 한 번에 조회
    @Query("SELECT p FROM Product p JOIN FETCH p.seller WHERE p.seller.id = :sellerId ORDER BY p.id DESC")
    List<Product> findProductsBySellerId(@Param("sellerId") Long sellerId);

    // 재고 확인과 차감을 한 SQL로 처리해 동시 주문 시 초과 판매를 막음
    @Modifying(flushAutomatically = true)
    @Query(value = """
            update products
            set quantity = quantity - :quantity,
                sales_count = sales_count + :quantity,
                is_sold_out = (quantity - :quantity) <= 0
            where id = :productId
              and quantity >= :quantity
              and :quantity > 0
            """, nativeQuery = true)
    int decreaseStockIfEnough(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
