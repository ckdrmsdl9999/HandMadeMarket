package com.project.marketplace.order.repository;

import com.project.marketplace.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems where o.orderId = :orderId")
    Optional<Order> findDetailById(@Param("orderId") Long orderId);

    @Query("select distinct o from Order o left join fetch o.orderItems where o.userId = :userId order by o.orderDate desc")
    List<Order> findDetailsByUserId(@Param("userId") Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems order by o.orderDate desc")
    List<Order> findAllDetailsOrderByOrderDateDesc();
}