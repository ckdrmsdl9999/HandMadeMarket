package com.project.marketplace.order.service;

import com.project.marketplace.cart.entity.Cart;
import com.project.marketplace.cart.entity.CartItem;
import com.project.marketplace.cart.repository.CartRepository;
import com.project.marketplace.order.dto.OrderCreateRequestDto;
import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.dto.OrderResponseDto;
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
    // 주문 생성/수정 시 userId를 실제 사용자로 검증해 연관관계 무결성을 보장하기 위해 사용자 저장소를 추가했다.
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

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
        return savedOrder.getOrderId();
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    public OrderResponseDto getOrderById(Long orderId) {
        // 주문 상세 응답은 주문상품 목록까지 포함해야 하므로 fetch join 조회 결과를 응답 DTO로 변환함
        return orderRepository.findDetailById(orderId)
                .map(OrderResponseDto::fromEntity)
                .orElse(null);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    public OrderResponseDto getOrderByOrderNumber(String orderNumber) {
        // 주문번호 조회도 주문상품 목록을 포함한 응답 DTO로 내려주도록 변경함
        return orderRepository.findDetailByOrderNumber(orderNumber)
                .map(OrderResponseDto::fromEntity)
                .orElse(null);
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

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        // 주문 취소는 주문 기록을 삭제하지 않고 상태와 재고만 되돌리도록 처리함
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }

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
}
