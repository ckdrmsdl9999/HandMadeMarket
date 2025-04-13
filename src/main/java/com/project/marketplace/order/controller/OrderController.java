package com.project.marketplace.order.controller;

import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.service.OrderService;
import com.project.marketplace.product.dto.ProductDto;
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
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        OrderDto orderDto = orderService.getOrderById(orderId);
        if (orderDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderDto);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    @GetMapping("/number/{orderNumber}") // /api/orders/number/{orderNumber}
    public ResponseEntity<OrderDto> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderDto orderDto = orderService.getOrderByOrderNumber(orderNumber);
        if (orderDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderDto);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    @GetMapping("/user/{userId}") // /api/orders/user/{userId}
    public ResponseEntity<List<OrderDto>> getOrdersByUserId(@PathVariable Long userId) {
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 모든 주문을 조회합니다.
     */
    @GetMapping // /api/orders
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        List<OrderDto> orders = orderService.getAllOrders();
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

        orderService.updateOrderStatus(orderId, orderStatus);
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