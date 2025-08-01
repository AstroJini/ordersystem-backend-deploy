package com.beyond.ordersystem.product.domain;

import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Entity
@Builder
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void updateImageUrl(String imageUrl) {
        this.imagePath = imageUrl;
    }

    public void updateProduct(ProductUpdateDto productUpdateDto) {
        this.name = productUpdateDto.getName();
        this.price = productUpdateDto.getPrice();
        this.stockQuantity = productUpdateDto.getStockQuantity();
        this.category = productUpdateDto.getCategory();
    }

    public void updateStockQuantity(int orderQuantity) {
        this.stockQuantity = this.stockQuantity-orderQuantity;
    }

    public void cancelOrder(int orderQuantity) {
        this.stockQuantity = this.stockQuantity + orderQuantity;
    }
}
