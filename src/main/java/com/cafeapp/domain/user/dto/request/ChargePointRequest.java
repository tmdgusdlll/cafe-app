package com.cafeapp.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@Getter
public class ChargePointRequest {

    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다")
    private Long point;
}
