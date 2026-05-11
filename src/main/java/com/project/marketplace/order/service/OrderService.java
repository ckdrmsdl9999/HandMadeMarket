package com.project.marketplace.order.service;

import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.entity.Order;
import com.project.marketplace.order.entity.OrderStatus;
import com.project.marketplace.order.repository.OrderRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService{

    private final OrderRepository orderRepository;
    // 주문 생성/수정 시 userId를 실제 사용자로 검증해 연관관계 무결성을 보장하기 위해 사용자 저장소를 추가했다.
    private final UserRepository userRepository;

    /**
     * 새로운 주문을 생성합니다.
     */
    @Transactional
    public Long createOrder(OrderDto orderDto) {
        // 주문 생성 기본값을 서비스에서 보정해 클라이언트 입력 누락 시에도 일관된 데이터를 저장한다.
        // 주문번호 생성 (UUID 방식)
        String orderNumber = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        orderDto.setOrderNumber(orderNumber);

        // 주문 상태 초기화
        if (orderDto.getOrderStatus() == null) {
            orderDto.setOrderStatus(OrderStatus.PENDING);
        }

        // 주문 일시 설정
        if (orderDto.getOrderDate() == null) {
            orderDto.setOrderDate(LocalDateTime.now());
        }

        // 사용자 조회 전에 필수값 누락을 검사해 null ID로 인한 저장소 예외를 명확한 메시지로 바꾼다.
        if (orderDto.getUserId() == null) {
            throw new RuntimeException("주문자 ID는 필수입니다.");
        }
        // 주문 저장 전에 userId로 사용자 엔티티를 조회해 주문-사용자 FK가 유효한 경우만 저장되게 했다.
        User user = userRepository.findById(orderDto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + orderDto.getUserId()));
        // DTO를 엔티티로 변환한 뒤 사용자 연관관계를 연결해 JPA 매핑을 일관되게 적용한다.
        Order order = OrderDto.toEntity(orderDto);
        order.setUser(user);

        Order savedOrder = orderRepository.save(order);
        return savedOrder.getOrderId();
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    public OrderDto getOrderById(Long orderId) {
        // 조회 결과가 없을 수 있으므로 Optional을 DTO null 반환 규칙에 맞춰 변환한다.
        return orderRepository.findById(orderId)
                .map(OrderDto::fromEntity)
                .orElse(null);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        // 주문번호 조회도 JPA 메서드 기반으로 전환해 Mapper 의존을 제거한다.
        return orderRepository.findByOrderNumber(orderNumber)
                .map(OrderDto::fromEntity)
                .orElse(null);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    public List<OrderDto> getOrdersByUserId(Long userId) {
        // 사용자 주문 목록을 최신 주문 우선으로 내려주기 위해 정렬 메서드를 사용한다.
        // 주문 조회는 User 내부 PK를 기준으로 저장소 메서드를 호출하도록 수정했다 -3/16
        return orderRepository.findByUser_IdOrderByOrderDateDesc(userId)
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 모든 주문을 조회합니다.
     */
    public List<OrderDto> getAllOrders() {
        // 전체 주문도 엔티티 목록을 DTO로 변환해 컨트롤러 응답 형식을 유지한다.
        return orderRepository.findAll()
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 주문 상태를 업데이트합니다.
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus orderStatus) {
        // 상태 변경 시 대상 주문을 조회해 엔티티를 수정하고 JPA dirty checking으로 반영한다.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderId));


            order.setOrderStatus(orderStatus);


    }

    /**
     * 주문 정보를 업데이트합니다.
     */
    @Transactional
    public void updateOrder(OrderDto orderDto) {
        // 전체 주문 수정은 기존 엔티티를 기준으로 필요한 필드만 갱신해 의도치 않은 덮어쓰기를 줄인다.
        Order order = orderRepository.findById(orderDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderDto.getOrderId()));
        // 주문 수정에서도 주문자 ID 누락을 선제 차단해 잘못된 사용자 연관갱신 요청을 막는다.
        if (orderDto.getUserId() == null) {
            throw new RuntimeException("주문자 ID는 필수입니다.");
        }
        // 수정 요청의 userId도 사용자 엔티티로 검증해 주문 소유자 연관관계를 안전하게 갱신한다.
        User user = userRepository.findById(orderDto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + orderDto.getUserId()));
        order.setUser(user);
        order.setOrderNumber(orderDto.getOrderNumber());
        order.setOrderStatus(orderDto.getOrderStatus());
        order.setTotalAmount(orderDto.getTotalAmount());
        order.setOrderDate(orderDto.getOrderDate());
        order.setRecipientName(orderDto.getRecipientName());
        order.setRecipientPhone(orderDto.getRecipientPhone());
        order.setShippingAddress(orderDto.getShippingAddress());
    }

    /**
     * 주문을 삭제합니다.
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        // 삭제도 Repository 단일 경로로 통일해 데이터 접근 계층을 JPA로 일원화한다.
        orderRepository.deleteById(orderId);
    }
}
