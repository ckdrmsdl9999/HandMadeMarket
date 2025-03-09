package com.project.marketplace.order.service;

import com.project.marketplace.order.dto.OrderDto;
import com.project.marketplace.order.mapper.OrderMapper;
import com.project.marketplace.product.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService{

    private final OrderMapper orderMapper;

    /**
     * 새로운 주문을 생성합니다.
     */
    @Transactional
    public Long createOrder(OrderDto orderDto) {
        // 주문번호 생성 (UUID 방식)
        String orderNumber = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        orderDto.setOrderNumber(orderNumber);

        // 주문 상태 초기화
        if (orderDto.getOrderStatus() == null) {
            orderDto.setOrderStatus("PENDING");
        }

        // 주문 일시 설정
        if (orderDto.getOrderDate() == null) {
            orderDto.setOrderDate(LocalDateTime.now());
        }

        orderMapper.insertOrder(orderDto);
        return orderDto.getOrderId();
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     */
    public OrderDto getOrderById(Long orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    /**
     * 주문번호로 주문을 조회합니다.
     */
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        return orderMapper.selectOrderByOrderNumber(orderNumber);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     */
    public List<OrderDto> getOrdersByUserId(Long userId) {
        return orderMapper.selectOrdersByUserId(userId);
    }

    /**
     * 모든 주문을 조회합니다.
     */
    public List<OrderDto> getAllOrders() {
        return orderMapper.selectAllOrders();
    }

    /**
     * 주문 상태를 업데이트합니다.
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String orderStatus) {
        orderMapper.updateOrderStatus(orderId, orderStatus);
    }

    /**
     * 주문 정보를 업데이트합니다.
     */
    @Transactional
    public void updateOrder(OrderDto orderDto) {
        orderMapper.updateOrder(orderDto);
    }

    /**
     * 주문을 삭제합니다.
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        orderMapper.deleteOrder(orderId);
    }
}
