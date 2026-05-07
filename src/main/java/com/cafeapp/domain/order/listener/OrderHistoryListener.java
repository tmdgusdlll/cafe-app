package com.cafeapp.domain.order.listener;

import com.cafeapp.common.DataCollectPlatformClient;
import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.cafeapp.common.config.kafka.topic.KafkaTopic.TOPIC_ORDER_COMPLETED;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderHistoryListener {

    private final DataCollectPlatformClient dataCollectPlatformClient;

    @KafkaListener(
            topics = TOPIC_ORDER_COMPLETED,
            groupId = "order-history-group",
            containerFactory = "orderHistoryKafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {
        // 외부 데이터 수집 플랫폼으로 전송 (Mock)
        dataCollectPlatformClient.send(
                event.getUserId(),
                event.getMenuId(),
                event.getQuantity(),
                event.getAmount(),
                event.getPaidAt()
        );
    }
}
