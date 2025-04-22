package com.project.marketplace.delivery.controller;

import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery")
public class DeliveryController {
    private final DeliveryService deliveryService;

    @PostMapping
    public Delivery createDelivery(@RequestBody Delivery delivery) {
        return deliveryService.saveDelivery(delivery);
    }

    @GetMapping
    public List<Delivery> getAllDeliveries() {
        return deliveryService.findAllDelivery();
    }

    @GetMapping("/{id}")
    public Delivery getOneDelivery(@PathVariable Long id) {
        return deliveryService.findByIdDelivery(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }

    @PutMapping("/{id}")
    public Delivery updateDelivery(@PathVariable Long id, @RequestBody Delivery delivery) {
        return deliveryService.updateDelivery(id, delivery);
    }

    @DeleteMapping("/{id}")
    public void deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
    }

}
