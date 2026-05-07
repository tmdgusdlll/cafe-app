package com.cafeapp.domain.order.dto.response;

import com.cafeapp.domain.order.entity.Order;
import com.cafeapp.domain.order.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderResponse {

    private final Long orderId;
    private final Long userId;
    private final Long menuId;
    private final String menuName;
    private final Long amount;
    private final OrderStatus status;
    private final Long remainingPoint;
    private final LocalDateTime orderedAt;

    private OrderResponse(Order order, LocalDateTime orderedAt) {
        this.orderId = order.getId();
        this.userId = order.getUser().getId();
        this.menuId = order.getMenu().getId();
        this.menuName = order.getMenu().getMenuName();
        this.amount = order.getAmount();
        this.status = order.getStatus();
        this.remainingPoint = order.getUser().getPoint();
        this.orderedAt = orderedAt;
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order, order.getCreatedAt());
    }
}
