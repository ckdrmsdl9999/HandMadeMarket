package com.project.marketplace.delivery.repository;

import com.project.marketplace.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository  extends JpaRepository<Delivery, Long>  {

}
