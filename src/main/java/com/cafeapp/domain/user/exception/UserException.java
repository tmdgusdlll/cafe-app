package com.cafeapp.domain.user.exception;

import com.cafeapp.global.exception.CafeException;
import com.cafeapp.global.exception.ErrorCode;

public class UserException extends CafeException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
