package com.cafeapp.domain.order.exception;

import com.cafeapp.global.exception.CafeException;
import com.cafeapp.global.exception.ErrorCode;

public class OrderException extends CafeException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
