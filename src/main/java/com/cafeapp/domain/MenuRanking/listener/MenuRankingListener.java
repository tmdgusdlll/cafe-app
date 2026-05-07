package com.cafeapp.domain.MenuRanking.listener;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.domain.MenuRanking.service.MenuRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.cafeapp.common.config.kafka.topic.KafkaTopic.TOPIC_ORDER_COMPLETED;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuRankingListener {

    private final MenuRankingService menuRankingService;

    @KafkaListener(
            topics = TOPIC_ORDER_COMPLETED,
            groupId = "menu-ranking-group",
            containerFactory = "menuRankingKafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {
        menuRankingService.increaseMenuRanking(
                event.getMenuId(),
                event.getQuantity(),
                LocalDate.now()
        );
    }
}
