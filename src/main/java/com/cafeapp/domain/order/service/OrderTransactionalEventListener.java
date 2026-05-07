package com.cafeapp.domain.order.service;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.domain.order.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionalEventListener {

    private final OrderProducer orderProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        // 커밋 확인 후 Kafka 발행
        orderProducer.send(event);
    }
}
