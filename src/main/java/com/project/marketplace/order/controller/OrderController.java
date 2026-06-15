package com.project.marketplace.order.controller;

import com.project.marketplace.order.dto.OrderCreateRequestDto;
import com.project.marketplace.order.dto.OrderResponseDto;
import com.project.marketplace.order.dto.OrderUpdateRequestDto;
import com.project.marketplace.order.entity.OrderStatus;
import com.project.marketplace.order.service.OrderService;
import com.project.marketplace.user.entity.User;
import com.project.marketplace.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
// Swagger 문서에서 주문 API를 도메인별로 묶기 위해 태그를 지정함
@Tag(name = "Order", description = "주문 API")
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
        User user = getAuthenticatedUser(authentication, "로그인 후 주문할 수 있습니다.");
        Long orderId = orderService.createOrder(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderId", orderId));
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    @GetMapping("/{orderId}") // /api/orders/{orderId}
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        // 주문 상세 조회는 현재 로그인 사용자 소유 주문만 보이도록 제한함
        User user = getAuthenticatedUser(authentication, "로그인 후 주문을 조회할 수 있습니다.");
        OrderResponseDto order = orderService.getOrderById(orderId, user.getId());
        return ResponseEntity.ok(order);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    @GetMapping("/number/{orderNumber}") // /api/orders/number/{orderNumber}
    public ResponseEntity<OrderResponseDto> getOrderByOrderNumber(@PathVariable String orderNumber, Authentication authentication) {
        // 주문번호 조회도 현재 로그인 사용자 소유 주문만 보이도록 제한함
        User user = getAuthenticatedUser(authentication, "로그인 후 주문을 조회할 수 있습니다.");
        OrderResponseDto order = orderService.getOrderByOrderNumber(orderNumber, user.getId());
        return ResponseEntity.ok(order);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    @GetMapping("/me") // /api/orders/me
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        // 주문 목록 조회는 경로 userId 대신 현재 로그인 사용자 기준으로 처리함
        User user = getAuthenticatedUser(authentication, "로그인 후 주문 목록을 조회할 수 있습니다.");
        List<OrderResponseDto> orders = orderService.getOrdersByUserId(user.getId());
        return ResponseEntity.ok(orders);
    }

    /**
     * 모든 주문을 조회합니다.
     */
    @GetMapping // /api/orders
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        // 전체 주문 목록 관리자 제한은 SecurityConfig에서 처리함
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

        // 주문 상태 변경 관리자 제한은 SecurityConfig에서 처리함
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
            Authentication authentication,
            @RequestBody OrderUpdateRequestDto request) {

        // 주문 수정은 현재 로그인 사용자 소유 주문의 수령 정보만 변경하도록 제한함
        User user = getAuthenticatedUser(authentication, "로그인 후 주문을 수정할 수 있습니다.");
        orderService.updateOrder(orderId, user.getId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 주문을 취소합니다.
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        // 주문 취소는 삭제가 아니라 현재 로그인 사용자의 주문 상태 변경으로 처리함
        User user = getAuthenticatedUser(authentication, "로그인 후 주문을 취소할 수 있습니다.");
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok().build();
    }

    // 인증 사용자 조회를 한 곳으로 모아 일반/OAuth 로그인 식별 처리를 재사용함
    private User getAuthenticatedUser(Authentication authentication, String unauthorizedMessage) {
        return userService.getAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage));
    }

}
