package com.cafeapp.domain.user.dto.response;

import com.cafeapp.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class ChargePointResponse {

    private final Long userId;
    private final Long point;

    private ChargePointResponse(Long userId, Long point) {
        this.userId = userId;
        this.point = point;
    }

    public static ChargePointResponse from(User user) {
        return new ChargePointResponse(user.getId(), user.getPoint());
    }
}
