package com.project.marketplace.delivery.service;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.entity.DeliveryStatus;
import com.project.marketplace.delivery.repository.DeliveryRepository;
import com.project.marketplace.order.entity.Order;
import com.project.marketplace.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    // 컨트롤러가 엔티티 대신 DTO만 주고받을 수 있도록 생성 전용 DTO 메서드를 추가한다.
    @Transactional
    public DeliveryUpdateResponseDto saveDeliveryWithDto(DeliveryUpdateRequestDto requestDto) {
        validateCreateRequest(requestDto);
        Order order = orderRepository.findById(requestDto.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (deliveryRepository.existsByOrder_OrderId(order.getOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 배송 정보가 등록된 주문입니다.");
        }

        Delivery savedDelivery = deliveryRepository.save(toEntity(requestDto, order));
        return DeliveryUpdateResponseDto.fromEntity(savedDelivery);
    }

    // 목록 조회에서도 엔티티를 직접 반환하지 않도록 DTO 리스트 변환 메서드를 제공한다.
    @Transactional(readOnly = true)
    public List<DeliveryUpdateResponseDto> findAllDeliveryWithDto() {
        return deliveryRepository.findAll().stream()
                .map(DeliveryUpdateResponseDto::fromEntity)
                .toList();
    }

    // 단건 조회 응답에서 엔티티 노출을 막기 위해 DTO 반환 메서드를 제공한다.
    @Transactional(readOnly = true)
    public DeliveryUpdateResponseDto findByIdDeliveryWithDto(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."));
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }

    // 수정 요청을 DTO로 받아 필요한 필드만 반영하도록 DTO 기반 업데이트 메서드를 추가한다.
    @Transactional
    public DeliveryUpdateResponseDto updateDeliveryWithDto(Long id, DeliveryUpdateRequestDto requestDto) {
        validateUpdateRequest(requestDto);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."));

        delivery.setAddress(requestDto.getAddress());
        delivery.setStatus(requestDto.getStatus());
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }

    @Transactional
    public void deleteDelivery(Long id) {
        deliveryRepository.deleteById(id);
    }

    // DTO에서 허용한 필드만 엔티티로 옮겨 과다 바인딩 가능성을 줄인다.
    private Delivery toEntity(DeliveryUpdateRequestDto requestDto, Order order) {
        return Delivery.builder()
                .order(order)
                .address(requestDto.getAddress())
                .status(requestDto.getStatus() != null ? requestDto.getStatus() : DeliveryStatus.READY)
                .build();
    }

    // 배송 생성은 주문 연결과 주소가 필수이므로 저장 전에 검증함
    private void validateCreateRequest(DeliveryUpdateRequestDto requestDto) {
        if (requestDto == null || requestDto.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
        validateAddress(requestDto.getAddress());
    }

    // 배송 수정은 주소와 상태를 명확히 받아 잘못된 상태 저장을 막음
    private void validateUpdateRequest(DeliveryUpdateRequestDto requestDto) {
        if (requestDto == null || requestDto.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송 상태는 필수입니다.");
        }
        validateAddress(requestDto.getAddress());
    }

    // 빈 배송지가 DB에 저장되지 않도록 공통 검증함
    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지는 필수입니다.");
        }
    }
}
