package com.project.marketplace.order.repository;

import com.project.marketplace.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    // Order.user 연관필드를 기준으로 사용자 주문 목록을 조회하도록 메서드 경로를 변경했다.
    List<Order> findByUser_UserIdOrderByOrderDateDesc(Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems where o.orderId = :orderId")
    Optional<Order> findDetailById(@Param("orderId") Long orderId);

    // JPQL 조건도 userId 컬럼 직접참조 대신 user 연관객체 경로를 사용하도록 수정했다.
    @Query("select distinct o from Order o left join fetch o.orderItems where o.user.userId = :userId order by o.orderDate desc")
    List<Order> findDetailsByUserId(@Param("userId") Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems order by o.orderDate desc")
    List<Order> findAllDetailsOrderByOrderDateDesc();
}
