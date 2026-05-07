package com.cafeapp.domain.order.producer;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.cafeapp.common.config.kafka.topic.KafkaTopic.TOPIC_ORDER_COMPLETED;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, OrderCompletedEvent> orderCompletedEventKafkaTemplate;

    public void send(OrderCompletedEvent event) {
        orderCompletedEventKafkaTemplate.send(TOPIC_ORDER_COMPLETED, event);
        log.info("[Kafka] 이벤트 발행 - userId: {}, menuId: {}, amount: {}",
                event.getUserId(), event.getMenuId(), event.getAmount());
    }
}
