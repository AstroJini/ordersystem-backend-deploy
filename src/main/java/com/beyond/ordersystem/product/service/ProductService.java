package com.beyond.ordersystem.product.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberCreateDto;
import com.beyond.ordersystem.member.dto.MemberResDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductCreateDto;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    public final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    public Long save(ProductCreateDto productCreateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email  = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는사용자 입니다"));
        Product product = productRepository.save(productCreateDto.toEntity(member));

        if(!productCreateDto.getProductImage().isEmpty() && productCreateDto.getProductImage() != null) {
            String fileName = "product-"+product.getId()+"-imagepath-"+productCreateDto.getProductImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(productCreateDto.getProductImage().getContentType())
                    .build();

            try{
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productCreateDto.getProductImage().getBytes()));
            } catch(Exception e) {
                throw new IllegalArgumentException("이미지 업로드 실패");
            }
//            이미지삭제시
//            s3Client.deleteObject(a->a.bucket(bucket).key(fileName));

            String imgUrl = s3Client.utilities().getUrl(a->a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImageUrl(imgUrl);
        } else {
            product.updateImageUrl(null);
        }
//        상품등록시 redis 에 재고세팅
        stockInventoryService.makeStockQuantity(product.getId(), product.getStockQuantity());
        return product.getId();
    }

    @Transactional(readOnly = true)
    public Page<ProductResDto> findAll(Pageable pageable, ProductSearchDto productSearchdto){
        System.out.println(productSearchdto.getCategory());
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if(productSearchdto.getCategory() != null){
                    predicateList.add(criteriaBuilder.equal(root.get("category"),productSearchdto.getCategory()));

                }
                if(productSearchdto.getProductName() != null){
                    predicateList.add(criteriaBuilder.like(root.get("name"),"%"+productSearchdto.getProductName()+"%"));
                }
                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        Page<Product> productList = productRepository.findAll(specification,pageable);
        return productList.map(p->ProductResDto.fromEntity(p));
    }

    @Transactional(readOnly = true)
    public ProductResDto findById(Long id){
        Product product = productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당제품은 없는제품"));
        return ProductResDto.fromEntity(product);
    }

    public Long updateProduct(ProductUpdateDto productUpdateDto,Long id) {
        Product product =  productRepository.findById(id).orElseThrow(()->new NoSuchElementException("없는 상품입니다"));
        product.updateProduct(productUpdateDto);
        if(!productUpdateDto.getProductImage().isEmpty() && productUpdateDto.getProductImage() != null) {

//            기존이미지를 삭제 : 파일명으로 삭제
            String imgUrl = product.getImagePath();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
            }
//            신규이미지 등록
            String newFileName = "product-"+product.getId()+"-imagepath-"+productUpdateDto.getProductImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newFileName)
                    .contentType(productUpdateDto.getProductImage().getContentType())
                    .build();

            try{
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productUpdateDto.getProductImage().getBytes()));
            } catch(Exception e) {
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

            String newImgUrl = s3Client.utilities().getUrl(a->a.bucket(bucket).key(newFileName)).toExternalForm();
            product.updateImageUrl(newImgUrl);
        } else {
//            s3에서 이미지 삭제후 url 갱신
            String imgUrl = product.getImagePath();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
            }
            product.updateImageUrl(null);
        }

        return product.getId();
    }

}
