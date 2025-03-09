package com.project.marketplace.order.mapper;

import com.project.marketplace.order.dto.OrderDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {


    void insertOrder(OrderDto orderDto);


    OrderDto selectOrderById(Long orderId);
    OrderDto selectOrderByOrderNumber(String orderNumber);
    List<OrderDto> selectOrdersByUserId(Long userId);
    List<OrderDto> selectAllOrders();


    void updateOrderStatus(Long orderId, String orderStatus);


    void updateOrder(OrderDto orderDto);


    void deleteOrder(Long orderId);
}
