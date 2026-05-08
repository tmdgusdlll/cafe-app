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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

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
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> orderHistoryKafkaListenerContainerFactory(
            CommonErrorHandler commonErrorHandlerWithDLT
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderHistoryEventConsumerFactory());
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);

        return factory;
    }

    // 7일 인기 메뉴용
    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> menuRankingConsumerFactory() {
        return buildConsumerFactory("menu-ranking-group");
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> menuRankingKafkaListenerContainerFactory(
            CommonErrorHandler commonErrorHandlerWithDLT
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(menuRankingConsumerFactory());
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);
        return factory;
    }

    // DLT
    @Bean
    public CommonErrorHandler commonErrorHandlerWithDLT(
            KafkaTemplate<String, OrderCompletedEvent> orderCompletedEventKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(orderCompletedEventKafkaTemplate);

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
