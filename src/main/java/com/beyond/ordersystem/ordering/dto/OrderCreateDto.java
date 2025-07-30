package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderCreateDto {
    private Long productId;
    private Integer productCount;

    public OrderDetail toEntity(Product product, Ordering ordering) {
        return OrderDetail.builder()
                .ordering(ordering)
                .quantity(productCount)
                .product(product)
                .build();
    }


}
//    private Long storeId;
//    private String payment;
//    private List<ProductDetailDto> details;
//
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Data
//    static class ProductDetailDto {
//        private Long productId;
//        private Integer productCount;
//    }
//데이터 구조 : {"details":[ {"productId":1, "productCount":3 }, {"productId":2, "productCount":4 }],
//    "storeId": 1, "payment" : "kakako"}