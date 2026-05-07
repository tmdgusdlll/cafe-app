package com.cafeapp.global.exception;

import lombok.Getter;

@Getter
public class CafeException extends RuntimeException{

    private final ErrorCode errorCode;

    public CafeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
