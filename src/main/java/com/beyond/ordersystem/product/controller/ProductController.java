package com.beyond.ordersystem.product.controller;

import com.beyond.ordersystem.common.dto.CommonDto;
import com.beyond.ordersystem.member.dto.MemberCreateDto;
import com.beyond.ordersystem.product.dto.ProductCreateDto;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import com.beyond.ordersystem.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> save(@ModelAttribute @Valid ProductCreateDto productCreateDto) {
        productService.save(productCreateDto);
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(productCreateDto.getProductImage().getOriginalFilename())
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("상품등록완료")
                        .build()
                , HttpStatus.CREATED);

    }


    @GetMapping("/list")
    public ResponseEntity<?> findAll(Pageable pageable, ProductSearchDto productSearchDto) {
        Page<ProductResDto> productResDtosList = productService.findAll(pageable, productSearchDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(productResDtosList)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품목록조회완료")
                        .build()
                , HttpStatus.OK);

    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ProductResDto productResDto = productService.findById(id);
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(productResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품상세조회완료")
                        .build()
                , HttpStatus.OK);

    }

    @PutMapping("/update/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @ModelAttribute ProductUpdateDto productUpdateDto) {
        Long id = productService.updateProduct(productUpdateDto, productId);
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품정보수정완료")
                        .build()
                , HttpStatus.OK);
    }



}
