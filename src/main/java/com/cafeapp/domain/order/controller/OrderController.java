package com.cafeapp.domain.order.controller;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.domain.order.service.RedissonService;
import com.cafeapp.global.response.ApiResponse;
import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.dto.response.OrderResponse;
import com.cafeapp.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RedissonService redissonService;

    // 주문,결제 API
    @PostMapping("/users/{userId}/order")
    public ResponseEntity<ApiResponse<OrderResponse>> orderAndPay(
            @PathVariable Long userId,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.orderAndPay(userId, request)));
    }

    // 분산락 버전 (Redisson)
    @PostMapping("/users/{userId}/order/redisson")
    public ResponseEntity<ApiResponse<OrderResponse>> orderAndPayWithRedisson(
            @PathVariable Long userId,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(redissonService.orderAndPayWithRedisson(userId, request)));
    }
}
