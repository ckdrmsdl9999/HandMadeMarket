package com.project.marketplace.delivery.service;

import com.project.marketplace.delivery.dto.DeliveryUpdateRequestDto;
import com.project.marketplace.delivery.dto.DeliveryUpdateResponseDto;
import com.project.marketplace.delivery.entity.Delivery;
import com.project.marketplace.delivery.entity.DeliveryStatus;
import com.project.marketplace.delivery.repository.DeliveryRepository;
import com.project.marketplace.order.entity.Order;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    // 주문 생성 직후 기본 배송 정보를 자동 생성해 별도 배송 생성 API가 필요 없게 함
    @Transactional
    public void createReadyDelivery(Order order) {
        if (deliveryRepository.existsByOrder_OrderId(order.getOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 배송 정보가 등록된 주문입니다.");
        }

        deliveryRepository.save(Delivery.builder()
                .order(order)
                .address(order.getShippingAddress())
                .status(DeliveryStatus.READY)
                .build());
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
    public DeliveryUpdateResponseDto findByIdDeliveryWithDto(Long id, Long userId, boolean admin) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."));
        validateDeliveryViewer(delivery, userId, admin);
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }

    // 수정 요청을 DTO로 받아 필요한 필드만 반영하도록 DTO 기반 업데이트 메서드를 추가한다.
    @Transactional
    public DeliveryUpdateResponseDto updateDeliveryWithDto(Long id, Long userId, boolean admin, DeliveryUpdateRequestDto requestDto) {
        validateUpdateRequest(requestDto, admin);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."));

        validateDeliveryManager(delivery, userId, admin);

        if (admin) {
            delivery.setAddress(requestDto.getAddress());
        }
        delivery.setStatus(requestDto.getStatus());
        return DeliveryUpdateResponseDto.fromEntity(delivery);
    }

    @Transactional
    public void deleteDelivery(Long id) {
        deliveryRepository.deleteById(id);
    }

    // 배송 수정은 상태를 필수로 받고 주소는 관리자 수정일 때만 필수로 검증함
    private void validateUpdateRequest(DeliveryUpdateRequestDto requestDto, boolean admin) {
        if (requestDto == null || requestDto.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송 상태는 필수입니다.");
        }
        if (admin) {
            validateAddress(requestDto.getAddress());
        }
    }

    // 빈 배송지가 DB에 저장되지 않도록 공통 검증함
    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지는 필수입니다.");
        }
    }

    // 관리자가 아니면 본인 주문에 연결된 배송만 조회할 수 있도록 제한함
    private void validateDeliveryViewer(Delivery delivery, Long userId, boolean admin) {
        if (!admin && !delivery.getOrder().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다.");
        }
    }

    // 관리자가 아니면 주문 상품의 판매자만 배송 상태를 수정할 수 있도록 제한함
    private void validateDeliveryManager(Delivery delivery, Long userId, boolean admin) {
        if (admin) {
            return;
        }

        boolean sellerOwnsOrderProduct = delivery.getOrder().getOrderItems().stream()
                .map(orderItem -> productRepository.findById(orderItem.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 상품을 찾을 수 없습니다.")))
                .map(Product::getSeller)
                .anyMatch(seller -> seller.getId().equals(userId));

        if (!sellerOwnsOrderProduct) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 배송을 수정할 권한이 없습니다.");
        }
    }
}
