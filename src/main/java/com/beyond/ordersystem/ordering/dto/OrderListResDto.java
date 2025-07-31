package com.beyond.ordersystem.ordering.dto;


import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderListResDto {
    private Long id;
    private String memberEmail;
    private OrderStatus orderStatus;
    private List<OrderDetailResDto> orderDetailResDto;

    public static OrderListResDto fromEntity(Ordering ordering){
        return OrderListResDto.builder()
                .id(ordering.getId())
                .memberEmail(ordering.getMember().getEmail())
                .orderStatus(ordering.getOrderStatus())
                .orderDetailResDto(
                        ordering.getOrderDetailList().stream()
                                .map(o -> OrderDetailResDto.fromEntity(o))
                                .collect(Collectors.toList())
                )
                .build();

    }
}
