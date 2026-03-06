package com.project.marketplace.cart.repository;

import com.project.marketplace.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {


    Optional<Cart> findByUser_UserId(Long userId);

    @Query("select distinct c from Cart c " +
            "left join fetch c.cartItems ci " +
            "left join fetch ci.product " +
            "where c.user.userId = :userId")
    Optional<Cart> findDetailByUserId(@Param("userId") Long userId);
}
