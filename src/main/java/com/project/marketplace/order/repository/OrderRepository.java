package com.project.marketplace.order.repository;

import com.project.marketplace.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    // 주문 목록 조회는 API 경로와 같은 User 내부 PK 기준으로 맞춰 식별자 의미를 통일했다 -3/16
    List<Order> findByUser_IdOrderByOrderDateDesc(Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems where o.orderId = :orderId")
    Optional<Order> findDetailById(@Param("orderId") Long orderId);

    // 주문번호 조회 응답에서도 주문상품 목록을 함께 내려주기 위해 상세 조회 쿼리를 분리함
    @Query("select distinct o from Order o left join fetch o.orderItems where o.orderNumber = :orderNumber")
    Optional<Order> findDetailByOrderNumber(@Param("orderNumber") String orderNumber);

    // 상세 주문 목록도 User.loginId가 아니라 내부 PK로 조회되게 조건을 정리했다 -3/16
    @Query("select distinct o from Order o left join fetch o.orderItems where o.user.id = :userId order by o.orderDate desc")
    List<Order> findDetailsByUserId(@Param("userId") Long userId);

    @Query("select distinct o from Order o left join fetch o.orderItems order by o.orderDate desc")
    List<Order> findAllDetailsOrderByOrderDateDesc();
}
