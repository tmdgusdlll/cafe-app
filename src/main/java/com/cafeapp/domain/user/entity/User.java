package com.cafeapp.domain.user.entity;

import com.cafeapp.domain.user.exception.UserException;
import com.cafeapp.global.entity.BaseEntity;
import com.cafeapp.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long point;

    public User(String name, Long point) {
        this.name = name;
        this.point = point;
    }

    // 포인트 충전
    public void chargePoint(Long amount) {
        this.point += amount;
    }

    // 포인트 결제
    public void usePoint(Long amount) {
        if (this.point < amount) {
            throw new UserException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= amount;
    }
}
