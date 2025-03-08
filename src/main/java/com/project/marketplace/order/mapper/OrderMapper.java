package com.project.marketplace.order.mapper;

import com.project.marketplace.order.dto.OrderDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 새 주문을 등록합니다.
     */
    int insertOrder(OrderDto orderDto);



}
