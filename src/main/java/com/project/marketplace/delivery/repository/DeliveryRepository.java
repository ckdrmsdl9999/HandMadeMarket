package com.project.marketplace.delivery.repository;

import com.project.marketplace.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository  extends JpaRepository<Delivery, Long>  {

    Optional<Delivery> findByOrder_OrderId(Long orderId);

    boolean existsByOrder_OrderId(Long orderId);
}
