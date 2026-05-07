package com.cafeapp.domain.order.entity;

import com.cafeapp.global.entity.BaseEntity;
import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    public Order(User user, Menu menu, Long amount) {
        this.user = user;
        this.menu = menu;
        this.amount = amount;
        this.status = OrderStatus.PENDING;
    }

    // 주문,결제 완료시 상태 변경
    public void complete() {
        this.status = OrderStatus.PAYMENT_COMPLETED;
    }
}
