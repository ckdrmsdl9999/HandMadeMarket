package com.project.marketplace.delivery.controller;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
import com.project.marketplace.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery")
public class DeliveryController {
    private final DeliveryService deliveryService;



    @PostMapping
    public ResponseEntity<DeliveryUpdateResponseDto> createDelivery(@RequestBody DeliveryUpdateRequestDto requestDto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.saveDeliveryWithDto(requestDto));
    }



    @GetMapping
    public ResponseEntity<List<DeliveryUpdateResponseDto>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.findAllDeliveryWithDto());
    }



    @GetMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> getOneDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.findByIdDeliveryWithDto(id));
    }



    @PutMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> updateDelivery(@PathVariable Long id, @RequestBody DeliveryUpdateRequestDto requestDto) {
        return ResponseEntity.ok(deliveryService.updateDeliveryWithDto(id, requestDto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }


}
