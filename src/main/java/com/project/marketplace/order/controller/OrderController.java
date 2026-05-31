package com.project.marketplace.order.controller;

import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.dto.OrderResponseDto;
import com.project.marketplace.order.entity.OrderStatus;
import com.project.marketplace.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders") // 이 부분을 추가해서 기본 경로 설정
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 새로운 주문을 생성합니다.
     */
    @PostMapping // /api/orders
    public ResponseEntity<Map<String, Long>> createOrder(@RequestBody OrderDto orderDto) {
        Long orderId = orderService.createOrder(orderDto);
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
     * 주문을 삭제합니다.
     */
    @DeleteMapping("/{orderId}") // /api/orders/{orderId}
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
