package com.cafeapp.common.config.kafka.consumer;

import com.cafeapp.common.config.kafka.event.OrderCompletedEvent;
import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServers;

    // 공통메서드
    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return props;
    }

    // 공통메서드
    private ConsumerFactory<String, OrderCompletedEvent> buildConsumerFactory(String groupId) {
        JacksonJsonDeserializer<OrderCompletedEvent> deserializer = new JacksonJsonDeserializer<>(OrderCompletedEvent.class);

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(groupId),
                new StringDeserializer(),
                deserializer
        );
    }
    // 주문 내역 보내기용
    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderHistoryEventConsumerFactory() {
        return buildConsumerFactory("order-history-group");
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> orderHistoryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderHistoryEventConsumerFactory());

        return factory;
    }

    // 7일 인기 메뉴용
    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> menuRankingConsumerFactory() {
        return buildConsumerFactory("menu-ranking-group");
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> menuRankingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(menuRankingConsumerFactory());
        return factory;
    }
}
