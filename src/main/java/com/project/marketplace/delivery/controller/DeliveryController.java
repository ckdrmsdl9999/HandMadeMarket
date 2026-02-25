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
        // 컨트롤러가 엔티티를 직접 다루지 않도록 서비스 DTO 메서드로 위임한다.
        return deliveryService.saveDeliveryWithDto(requestDto);
    }


    @GetMapping
    public List<DeliveryUpdateResponseDto> getAllDeliveries() {
        // 목록 조회도 서비스에서 DTO로 변환한 결과를 그대로 반환한다.
        return deliveryService.findAllDeliveryWithDto();
    }


    @GetMapping("/{id}")
    public DeliveryUpdateResponseDto getOneDelivery(@PathVariable Long id) {
        // 단건 조회도 컨트롤러에서 엔티티 변환 없이 DTO 응답만 반환한다.
        return deliveryService.findByIdDeliveryWithDto(id);
    }


    @PutMapping("/{id}")
    public DeliveryUpdateResponseDto updateDelivery(@PathVariable Long id, @RequestBody DeliveryUpdateRequestDto requestDto) {
        // 수정 처리도 DTO 입력을 그대로 서비스로 넘겨 엔티티 의존을 줄인다.
        return deliveryService.updateDeliveryWithDto(id, requestDto);
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
