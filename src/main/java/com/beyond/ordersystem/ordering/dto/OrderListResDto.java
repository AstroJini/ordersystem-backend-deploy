package com.beyond.ordersystem.ordering.dto;


import com.beyond.ordersystem.ordering.domain.OrderDetail;
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
    private String orderStatus;
    private List<OrderDetailList> orderDetailList;

    public static OrderListResDto fromEntity(Ordering ordering){
        return OrderListResDto.builder()
                .id(ordering.getId())
                .memberEmail(ordering.getMember().getEmail())
                .orderStatus(ordering.getOrderStatus().toString())
                .orderDetailList(
                        ordering.getOrderDetailList().stream()
                                .map(o -> OrderDetailList.fromEntity(o))
                                .collect(Collectors.toList())
                )
                .build();

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    static class OrderDetailList {
        private Long detailId;
        private String productName;
        private Integer productCount;

        public static OrderDetailList fromEntity(OrderDetail orderDetail){
            return OrderDetailList.builder()
                    .detailId(orderDetail.getId())
                    .productName(orderDetail.getProduct().getName())
                    .productCount(orderDetail.getQuantity())
                    .build();
        }
    }


}
