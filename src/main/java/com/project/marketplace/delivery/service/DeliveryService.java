package com.project.marketplace.delivery.service;

import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public Delivery saveDelivery(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    public List<Delivery> findAllDelivery() {
        return deliveryRepository.findAll();
    }

    public Optional<Delivery> findByIdDelivery(Long id) {
        return deliveryRepository.findById(id);
    }

    public Delivery updateDelivery(Long id, Delivery updatedDelivery) {
        return deliveryRepository.findById(id)
                .map(delivery -> {
                    delivery.setAddress(updatedDelivery.getAddress());
                    delivery.setStatus(updatedDelivery.getStatus());
                    return deliveryRepository.save(delivery);
                })
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }

    public void deleteDelivery(Long id) {
        deliveryRepository.deleteById(id);
    }

}
