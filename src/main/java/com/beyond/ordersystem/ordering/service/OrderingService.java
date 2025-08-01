package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.common.service.StockRabbitMqService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberResDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderDetailResDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final MemberRepository memberRepository;
    private final StockInventoryService stockInventoryService;
    private final StockRabbitMqService  stockRabbitMqService;
    private final SseAlarmService  sseAlarmService;

    public Long create(List<OrderCreateDto> orderCreateDtoList) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email  = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는사용자 입니다"));

        Ordering ordering =  Ordering.builder()
                .member(member)
                .build();

        for(OrderCreateDto orderCreateDto : orderCreateDtoList) {
            Product product = productRepository.findById(orderCreateDto.getProductId()).orElseThrow(()->new EntityNotFoundException(""));
            if(product.getStockQuantity() < orderCreateDto.getProductCount()){
//                예외를 강제발생 시킴으로서, 모든 임시저장사항들을 rollback 처리
                throw new IllegalArgumentException("재고가 부족합니다");
            }
//            1. 동시에 접근하는 상황에서 update 값의 정합성이 깨지고 갱신이상이 발생
//            2. spring 버전이나 mysql 버전에 따라 jpa 에서 강제에러(deadlock) 를 유발시켜 대부분의 요청실패 발생
            product.updateStockQuantity(orderCreateDto.getProductCount());
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(orderCreateDto.getProductCount())
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetailList().add(orderDetail);
//            orderDetailRepository.save(orderDetail);
        }
        orderingRepository.save(ordering);
        return ordering.getId();
    }

//
    public List<OrderListResDto> findAll(){
        return orderingRepository.findAll().stream()
                .map(o->OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }


    public List<OrderListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("member is not found"));
        return  orderingRepository.findAllByMember(member).stream()
                .map(o->OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }

//    public List<OrderListResDto> findAll(){
//        List<Ordering> orderingList = orderingRepository.findAll();
//        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
//        for (Ordering ordering : orderingList){
//            List<OrderDetail> orderDetailList = ordering.getOrderDetailList();
//            List<OrderDetailResDto> orderDetailResDtoList = new ArrayList<>();
//            for (OrderDetail orderDetail : orderDetailList){
//                OrderDetailResDto orderDetailResDto = OrderDetailResDto.builder()
//                        .detailId(orderDetail.getId())
//                        .productName(orderDetail.getProduct().getName())
//                        .productCount(orderDetail.getQuantity())
//                        .build();
//                orderDetailResDtoList.add(orderDetailResDto);
//            }
//            OrderListResDto dto = OrderListResDto.builder()
//                    .id(ordering.getId())
//                    .memberEmail(ordering.getMember().getEmail())
//                    .orderStatus(ordering.getOrderStatus())
//                    .orderDetails(orderDetailResDtoList)
//                    .build();
//
//            orderListResDtoList.add(dto);
//        }
//
//        return  orderListResDtoList;
//    }
    @Transactional(isolation = Isolation.READ_COMMITTED) // 격리레벨을 낮춤으로서, 성능향상과 lock 관련 문제 원천차단한다.
    public Long createConcurrent(List<OrderCreateDto> orderCreateDtoList) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email  = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는사용자 입니다"));

        Ordering ordering =  Ordering.builder()
                .member(member)
                .build();

        for(OrderCreateDto orderCreateDto : orderCreateDtoList) {
            Product product = productRepository.findById(orderCreateDto.getProductId()).orElseThrow(()->new EntityNotFoundException(""));
//            redis에서 재고수량 확인 및 재고수량 감소처리
            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), orderCreateDto.getProductCount());
            if(newQuantity < 0){
                throw new IllegalArgumentException("재고부족");
            }
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(orderCreateDto.getProductCount())
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetailList().add(orderDetail);
//            orderDetailRepository.save(orderDetail);
            stockRabbitMqService.publish(orderCreateDto.getProductId(), orderCreateDto.getProductCount());
        }
        orderingRepository.save(ordering);

//        주문성공시 admin 에게 알림메세지발송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

        return ordering.getId();
    }

    public Ordering cancel(Long id){
//        ordering 에 상태값 변경 canceled
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException(""));
        ordering.cancelStatus();
        for(OrderDetail orderDetail : ordering.getOrderDetailList()){
//        redis의 재고값 증가
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
//        rdb 재고 업데이트
            stockInventoryService.increaseStockQuantity(orderDetail.getProduct().getId(), orderDetail.getQuantity());
        }
        return ordering;
    }

}
