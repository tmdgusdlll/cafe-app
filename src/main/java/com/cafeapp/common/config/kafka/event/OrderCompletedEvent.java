package com.cafeapp.common.config.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {

    private Long userId;
    private Long menuId;
    private int quantity;
    private Long amount;
    private String paidAt;
}
