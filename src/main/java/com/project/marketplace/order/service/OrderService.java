package com.project.marketplace.order.service;

import com.project.marketplace.cart.entity.Cart;
import com.project.marketplace.cart.entity.CartItem;
import com.project.marketplace.cart.repository.CartRepository;
import com.project.marketplace.delivery.service.DeliveryService;
import com.project.marketplace.order.dto.OrderCreateRequestDto;
import com.project.marketplace.order.dto.OrderResponseDto;
import com.project.marketplace.order.dto.OrderUpdateRequestDto;
import com.project.marketplace.order.entity.Order;
import com.project.marketplace.order.entity.OrderItem;
import com.project.marketplace.order.entity.OrderStatus;
import com.project.marketplace.order.repository.OrderRepository;
import com.project.marketplace.product.entity.Product;
import com.project.marketplace.product.repository.ProductRepository;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService{

    private final OrderRepository orderRepository;
    // 주문 생성 시 userId를 실제 사용자로 검증해 연관관계 무결성을 보장함
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final DeliveryService deliveryService;

    /**
     * 새로운 주문을 생성합니다.
     */
    @Transactional
    public Long createOrder(Long userId, OrderCreateRequestDto request) {
        // 주문 생성 사용자는 요청 body 대신 인증 사용자 내부 PK로 조회해 조작 가능성을 제거함
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        // 주문 품목과 수량은 요청값 대신 현재 사용자 장바구니에서 가져와 가격/수량 조작을 막음
        Cart cart = cartRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new RuntimeException("장바구니가 비어 있습니다."));
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("장바구니가 비어 있습니다.");
        }

        Order order = Order.builder()
                .user(user)
                .orderNumber(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16))
                .orderStatus(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .totalAmount(0)
                .build();

        int totalAmount = 0;
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            int unitPrice = product.getPrice();

            if (product.getQuantity() < quantity) {
                throw new RuntimeException("상품 재고가 부족합니다. 상품명: " + product.getName());
            }

            order.addItem(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    unitPrice,
                    quantity
            ));
            totalAmount += unitPrice * quantity;

            product.setQuantity(product.getQuantity() - quantity);
            product.setSalesCount(product.getSalesCount() + quantity);
            product.setIsSoldOut(product.getQuantity() <= 0);
        }

        order.setTotalAmount(totalAmount);
        // 주문 생성 후 장바구니 항목을 비워 같은 상품이 중복 주문되지 않게 함
        List.copyOf(cart.getCartItems()).forEach(cart::removeCartItem);

        Order savedOrder = orderRepository.save(order);
        // 주문 생성과 동시에 기본 배송 정보를 생성해 배송 조회 흐름이 끊기지 않게 함
        deliveryService.createReadyDelivery(savedOrder);
        return savedOrder.getOrderId();
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    public OrderResponseDto getOrderById(Long orderId, Long userId) {
        // 주문 상세 조회는 현재 로그인 사용자 소유 주문만 DTO로 변환함
        return OrderResponseDto.fromEntity(findOrderForUser(orderId, userId));
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    public OrderResponseDto getOrderByOrderNumber(String orderNumber, Long userId) {
        // 주문번호 조회도 현재 로그인 사용자 소유 여부를 확인한 뒤 응답함
        Order order = orderRepository.findDetailByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        validateOrderOwner(order, userId);
        return OrderResponseDto.fromEntity(order);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        // 사용자 주문 목록을 최신 주문 우선으로 내려주기 위해 정렬 메서드를 사용한다.
        // 주문 조회는 User 내부 PK를 기준으로 저장소 메서드를 호출하도록 수정했다 -3/16
        return orderRepository.findDetailsByUserId(userId)
                .stream()
                .map(OrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 모든 주문을 조회합니다.
     */
    public List<OrderResponseDto> getAllOrders() {
        // 전체 주문 조회도 주문상품 목록을 포함한 응답 DTO로 내려주도록 변경함
        return orderRepository.findAllDetailsOrderByOrderDateDesc()
                .stream()
                .map(OrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 주문 상태를 업데이트합니다.
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus orderStatus) {
        // 상태 변경 시 대상 주문을 조회해 엔티티를 수정하고 JPA dirty checking으로 반영한다.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        order.setOrderStatus(orderStatus);
    }

    /**
     * 주문 정보를 업데이트합니다.
     */
    @Transactional
    public void updateOrder(Long orderId, Long userId, OrderUpdateRequestDto request) {
        // 주문 수정은 본인 주문의 수령 정보만 변경해 금액/상태/주문자 조작을 막음
        Order order = findOrderForUser(orderId, userId);
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대기 중인 주문만 수정할 수 있습니다.");
        }
        validateOrderUpdateRequest(request);

        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setShippingAddress(request.getShippingAddress());
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        // 주문 취소는 주문 기록을 삭제하지 않고 상태와 재고만 되돌리도록 처리함
        Order order = findOrderForUser(orderId, userId);

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "취소할 수 없는 주문 상태입니다.");
        }

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

            product.setQuantity(product.getQuantity() + orderItem.getQuantity());
            product.setSalesCount(Math.max(0, product.getSalesCount() - orderItem.getQuantity()));
            product.setIsSoldOut(product.getQuantity() <= 0);
        }

        order.setOrderStatus(OrderStatus.CANCELED);
    }

    // 주문 조회와 변경에서 본인 주문 확인을 같은 기준으로 처리함
    private Order findOrderForUser(Long orderId, Long userId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        validateOrderOwner(order, userId);
        return order;
    }

    // 다른 사용자의 주문 존재 여부가 노출되지 않도록 소유자가 아니면 404로 응답함
    private void validateOrderOwner(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
    }

    // 주문 수령 정보 필수값 누락으로 DB 제약조건 오류가 나지 않도록 요청 단계에서 차단함
    private void validateOrderUpdateRequest(OrderUpdateRequestDto request) {
        if (request == null
                || request.getRecipientName() == null || request.getRecipientName().isBlank()
                || request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()
                || request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수령 정보는 필수입니다.");
        }
    }
}
