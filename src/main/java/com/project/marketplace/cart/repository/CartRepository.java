package com.project.marketplace.cart.repository;

import com.project.marketplace.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {


    // 장바구니 소유자 조회를 문자열 loginId가 아닌 User 내부 PK 기준으로 맞췄다 -3/16
    Optional<Cart> findByUser_Id(Long userId);

    // 장바구니 상세 조회도 API 경로에서 넘기는 내부 PK로 사용자와 매칭되게 변경했다 -3/16
    @Query("select distinct c from Cart c " +
            "left join fetch c.cartItems ci " +
            "left join fetch ci.product " +
            "where c.user.id = :userId")
    Optional<Cart> findDetailByUserId(@Param("userId") Long userId);
}
