package com.cafeapp.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DataCollectPlatformClient {

    public void send(Long userId, Long menuId, int quantity, Long amount, String paidAt) {
        log.info("[데이터 수집 플랫폼] 전송 완료 - userId: {}, menuId: {}, quantity : {}, amount: {}, paidAt : {}",
                userId, menuId, quantity, amount, paidAt);
    }
}