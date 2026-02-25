package com.project.marketplace.delivery.controller;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery")
public class DeliveryController {
    private final DeliveryService deliveryService;


    @PostMapping
    public DeliveryUpdateResponseDto createDelivery(@RequestBody DeliveryUpdateRequestDto requestDto) {
        Delivery savedDelivery = deliveryService.saveDelivery(toEntity(requestDto));
        return DeliveryUpdateResponseDto.fromEntity(savedDelivery);
    }


    @GetMapping
    public List<DeliveryUpdateResponseDto> getAllDeliveries() {
        return deliveryService.findAllDelivery().stream()
                .map(DeliveryUpdateResponseDto::fromEntity)
                .collect(Collectors.toList());
    }


    @GetMapping("/{id}")
    public DeliveryUpdateResponseDto getOneDelivery(@PathVariable Long id) {
        Delivery delivery = deliveryService.findByIdDelivery(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }


    @PutMapping("/{id}")
    public DeliveryUpdateResponseDto updateDelivery(@PathVariable Long id, @RequestBody DeliveryUpdateRequestDto requestDto) {
        Delivery updatedDelivery = deliveryService.updateDelivery(id, toEntity(requestDto));
        return DeliveryUpdateResponseDto.fromEntity(updatedDelivery);
    }

    @DeleteMapping("/{id}")
    public void deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
    }


    private Delivery toEntity(DeliveryUpdateRequestDto requestDto) {
        return Delivery.builder()
                .address(requestDto.getAddress())
                .status(requestDto.getStatus())
                .build();
    }

}
