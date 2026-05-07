package com.cafeapp.domain.user.controller;

import com.cafeapp.global.response.ApiResponse;
import com.cafeapp.domain.user.dto.request.ChargePointRequest;
import com.cafeapp.domain.user.dto.response.ChargePointResponse;
import com.cafeapp.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 포인트 충전하기 API
    @PostMapping("/users/{userId}/point")
    public ResponseEntity<ApiResponse<ChargePointResponse>> chargePoint(
            @PathVariable Long userId,
            @Valid @RequestBody ChargePointRequest request
            ) {
        return ResponseEntity.ok(ApiResponse.success(userService.chargePoint(userId, request)));
    }
}
