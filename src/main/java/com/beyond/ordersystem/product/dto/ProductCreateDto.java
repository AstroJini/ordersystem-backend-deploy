package com.beyond.ordersystem.product.dto;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.product.domain.Product;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductCreateDto {
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private MultipartFile productImage;

    public Product toEntity(Member member) {
        return Product.builder()
                .name(name)
                .category(category)
                .price(price)
                .stockQuantity(stockQuantity)
                .member(member)
                .build();
    }
}
