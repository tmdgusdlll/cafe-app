package com.cafeapp.domain.order.service;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.common.config.redis.CacheManagerConfig;
import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.dto.response.OrderResponse;
import com.cafeapp.domain.order.entity.Order;
import com.cafeapp.domain.order.exception.OrderException;
import com.cafeapp.domain.order.producer.OrderProducer;
import com.cafeapp.domain.order.repository.OrderRepository;
import com.cafeapp.domain.pointTransaction.entity.PointTransaction;
import com.cafeapp.domain.pointTransaction.repository.PointTransactionRepository;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.repository.UserRepository;
import com.cafeapp.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final OrderProducer orderProducer;
    private final ApplicationEventPublisher eventPublisher;

    // 주문,결제 (재고 차감으로 품절될 수 있으므로 메뉴 목록 캐시 무효화)
    @CacheEvict(value = CacheManagerConfig.CACHE_NAME, key = "'#all'")
    @Transactional
    public OrderResponse orderAndPay(Long userId, OrderRequest request) {
        // user 확인 (비관적 락 적용)
        User user = userRepository.findByIdwithPessimisticLock(userId).orElseThrow(
                () -> new OrderException(ErrorCode.USER_NOT_FOUND)
        );
        // 락 X (동시성 재현용)
//        User user = userRepository.findById(userId).orElseThrow(
//                () -> new OrderException(ErrorCode.USER_NOT_FOUND)
//        );
        // menu 확인 (비관적 락 적용 - 다른 사용자의 동시 재고 차감 방지)
        Menu menu = menuRepository.findByIdWithPessimisticLock(request.getMenuId()).orElseThrow(
                () -> new OrderException(ErrorCode.MENU_NOT_FOUND)
        );
        // menu 상태 확인
        if (!menu.getStatus().equals(MenuStatus.ON_SALE)) {
            throw new OrderException(ErrorCode.MENU_NOT_ON_SALE);
        }
        // 결제 금액(포인트)
        Long amount = menu.getPrice() * request.getQuantity();

        // 주문 생성
        Order order = new Order(user, menu, amount);
        orderRepository.save(order);

        // 커피 재고 차감
        menu.decreaseStock(request.getQuantity());

        // 포인트 차감
        user.usePoint(order.getAmount());

        // 주문 상태 변경 (PENDING -> COMPLETED)
        order.complete();

        // 포인트 사용 이력 저장
        PointTransaction pt = PointTransaction.use(user, order, order.getAmount(), user.getPoint());
        pointTransactionRepository.save(pt);

        // 이벤트 큐에 일단 쌓아두고 대기 -> 커밋이 완료 되면 @TransactionalEventListener에 의해 handle() 실행
        // handle() 에서 orderProducer.send(event) 호출
        // DB와 Kafka 간의 데이터 정합성 보장
        eventPublisher.publishEvent(new OrderCompletedEvent(
                user.getId(),
                menu.getId(),
                request.getQuantity(),
                order.getAmount(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        return OrderResponse.from(order);
    }

    // Redisson 적용 (재고 차감으로 품절될 수 있으므로 메뉴 목록 캐시 무효화)
    @CacheEvict(value = CacheManagerConfig.CACHE_NAME, key = "'#all'")
    @Transactional
    public OrderResponse orderAndPayWithRedisson(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new OrderException(ErrorCode.USER_NOT_FOUND)
        );
        // menu 확인 (비관적 락 적용 - Redisson은 user-level 락이므로 Menu 재고는 DB 락으로 보호)
        Menu menu = menuRepository.findByIdWithPessimisticLock(request.getMenuId()).orElseThrow(
                () -> new OrderException(ErrorCode.MENU_NOT_FOUND)
        );
        // menu 상태 확인
        if (!menu.getStatus().equals(MenuStatus.ON_SALE)) {
            throw new OrderException(ErrorCode.MENU_NOT_ON_SALE);
        }
        // 결제 금액(포인트)
        Long amount = menu.getPrice() * request.getQuantity();

        // 주문 생성
        Order order = new Order(user, menu, amount);
        orderRepository.save(order);

        // 커피 재고 차감
        menu.decreaseStock(request.getQuantity());

        // 포인트 차감
        user.usePoint(order.getAmount());

        // 주문 상태 변경 (PENDING -> COMPLETED)
        order.complete();

        // 포인트 사용 이력 저장
        PointTransaction pt = PointTransaction.use(user, order, order.getAmount(), user.getPoint());
        pointTransactionRepository.save(pt);

        // 이벤트 큐에 일단 쌓아두고 대기 -> 커밋이 완료 되면 @TransactionalEventListener에 의해 handle() 실행
        // handle() 에서 orderProducer.send(event) 호출
        // DB와 Kafka 간의 데이터 정합성 보장
        eventPublisher.publishEvent(new OrderCompletedEvent(
                user.getId(),
                menu.getId(),
                request.getQuantity(),
                order.getAmount(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        return OrderResponse.from(order);
    }
}
