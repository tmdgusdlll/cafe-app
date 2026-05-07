package com.cafeapp.domain.pointTransaction.entity;

import com.cafeapp.global.entity.BaseEntity;
import com.cafeapp.domain.order.entity.Order;
import com.cafeapp.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long pointBalance; // 처리 후 잔액

    // 충전 기록
    public static PointTransaction charge(User user, Long amount, Long pointBalance) {
        PointTransaction tx = new PointTransaction();
        tx.user = user;
        tx.type = PointTransactionType.CHARGE;
        tx.amount = amount;
        tx.pointBalance = pointBalance;
        return tx;
    }

    // 사용 기록
    public static PointTransaction use(User user, Order order, Long amount, Long pointBalance) {
        PointTransaction tx = new PointTransaction();
        tx.user = user;
        tx.order = order;
        tx.type = PointTransactionType.USE;
        tx.amount = amount;
        tx.pointBalance = pointBalance;
        return tx;
    }
}
