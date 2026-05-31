package com.project.marketplace.order.controller;

import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.dto.OrderCreateRequestDto;
import com.project.marketplace.order.dto.OrderResponseDto;
import com.project.marketplace.order.entity.OrderStatus;
import com.project.marketplace.order.service.OrderService;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders") // 이 부분을 추가해서 기본 경로 설정
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * 새로운 주문을 생성합니다.
     */
    @PostMapping // /api/orders
    public ResponseEntity<Map<String, Long>> createOrder(
            Authentication authentication,
            @RequestBody OrderCreateRequestDto request) {
        // 주문자는 요청 body가 아니라 현재 로그인 사용자 기준으로 결정함
        User user = userService.getAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 주문할 수 있습니다."));
        Long orderId = orderService.createOrder(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderId", orderId));
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    @GetMapping("/{orderId}") // /api/orders/{orderId}
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {
        // 주문 조회 응답은 주문상품 목록을 포함한 전용 응답 DTO로 반환함
        OrderResponseDto order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    @GetMapping("/number/{orderNumber}") // /api/orders/number/{orderNumber}
    public ResponseEntity<OrderResponseDto> getOrderByOrderNumber(@PathVariable String orderNumber) {
        // 주문번호 조회도 같은 응답 DTO를 사용해 응답 구조를 통일함
        OrderResponseDto order = orderService.getOrderByOrderNumber(orderNumber);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    @GetMapping("/user/{userId}") // /api/orders/user/{userId}
    public ResponseEntity<List<OrderResponseDto>> getOrdersByUserId(@PathVariable Long userId) {
        // 주문 목록 응답도 주문상품 목록을 포함한 응답 DTO 리스트로 반환함
        List<OrderResponseDto> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 모든 주문을 조회합니다.
     */
    @GetMapping // /api/orders
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        // 전체 주문 목록도 응답 DTO를 사용해 요청 DTO와 역할을 분리함
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);

    }

    /**
     * 주문 상태를 업데이트합니다.
     */
    @PatchMapping("/{orderId}/status") // /api/orders/{orderId}/status
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> statusUpdate) {

        String orderStatus = statusUpdate.get("orderStatus");

        if (orderStatus == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            orderService.updateOrderStatus(orderId, OrderStatus.valueOf(orderStatus));
        }catch(IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 주문 정보를 업데이트합니다.
     */
    @PutMapping("/{orderId}") // /api/orders/{orderId}
    public ResponseEntity<Void> updateOrder(
            @PathVariable Long orderId,
            @RequestBody OrderDto orderDto) {

        if (!orderId.equals(orderDto.getOrderId())) {
            return ResponseEntity.badRequest().build();
        }

        orderService.updateOrder(orderDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 주문을 취소합니다.
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        // 주문 취소는 삭제가 아니라 현재 로그인 사용자의 주문 상태 변경으로 처리함
        User user = userService.getAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 주문을 취소할 수 있습니다."));
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok().build();
    }
}
