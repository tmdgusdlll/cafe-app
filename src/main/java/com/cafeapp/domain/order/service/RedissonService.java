package com.cafeapp.domain.order.service;

import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.dto.response.OrderResponse;
import com.cafeapp.domain.order.exception.OrderException;
import com.cafeapp.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedissonService {

    private final OrderService orderService;
    private final RedissonClient redissonClient;

    // Redisson 분산락 적용
    public OrderResponse orderAndPayWithRedisson(Long userId, OrderRequest request) {
        // RLock 객체 생성
        // getLock()은 Redis에 접근X
        // 키 이름과 연결된 락 객체만 반환
        // Redis 명령은 tryLock()시점에 발행
        String lockKey = "lock:order:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        // 락 획득 추적
        boolean acquired = false;

        try {
            // 3초 대기, watchDog 활성화
            acquired = lock.tryLock(3, -1, TimeUnit.SECONDS);

            // 락을 획득하지 못했다면 에러 (그대로 acquired = false)
            if (!acquired) {
                throw new OrderException(ErrorCode.TOO_MANY_REQUESTS);
            }
            // 락을 획득했다면 (acquired = true)
            return orderService.orderAndPayWithRedisson(userId, request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderException(ErrorCode.TOO_MANY_REQUESTS);
        } finally {
            // acquired = true이고 현재 스레드가 락 보유자인 경우에만 락 해제
            // isHeldByCurrentThread()로 다른 스레드 락 실수 해제 방지
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
