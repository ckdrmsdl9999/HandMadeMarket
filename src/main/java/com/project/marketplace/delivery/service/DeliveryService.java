package com.project.marketplace.delivery.service;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
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

    // 컨트롤러가 엔티티 대신 DTO만 주고받을 수 있도록 생성 전용 DTO 메서드를 추가한다.
    public DeliveryUpdateResponseDto saveDeliveryWithDto(DeliveryUpdateRequestDto requestDto) {
        Delivery savedDelivery = deliveryRepository.save(toEntity(requestDto));
        return DeliveryUpdateResponseDto.fromEntity(savedDelivery);
    }

    // 목록 조회에서도 엔티티를 직접 반환하지 않도록 DTO 리스트 변환 메서드를 제공한다.
    public List<DeliveryUpdateResponseDto> findAllDeliveryWithDto() {
        return deliveryRepository.findAll().stream()
                .map(DeliveryUpdateResponseDto::fromEntity)
                .toList();
    }

    // 단건 조회 응답에서 엔티티 노출을 막기 위해 DTO 반환 메서드를 제공한다.
    public DeliveryUpdateResponseDto findByIdDeliveryWithDto(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }

    // 수정 요청을 DTO로 받아 필요한 필드만 반영하도록 DTO 기반 업데이트 메서드를 추가한다.
    public DeliveryUpdateResponseDto updateDeliveryWithDto(Long id, DeliveryUpdateRequestDto requestDto) {
        return deliveryRepository.findById(id)
                .map(delivery -> {
                    delivery.setAddress(requestDto.getAddress());
                    delivery.setStatus(requestDto.getStatus());
                    return DeliveryUpdateResponseDto.fromEntity(deliveryRepository.save(delivery));
                })
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }


    public void deleteDelivery(Long id) {
        deliveryRepository.deleteById(id);
    }

    // DTO에서 허용한 필드만 엔티티로 옮겨 과다 바인딩 가능성을 줄인다.
    private Delivery toEntity(DeliveryUpdateRequestDto requestDto) {
        return Delivery.builder()
                .address(requestDto.getAddress())
                .status(requestDto.getStatus())
                .build();
    }

}
