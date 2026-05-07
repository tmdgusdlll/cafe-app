package com.cafeapp.domain.order.entity;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    REFUNDED
}
