package com.cafeapp.domain.menu.exception;

import com.cafeapp.global.exception.CafeException;
import com.cafeapp.global.exception.ErrorCode;

public class MenuException extends CafeException {
    public MenuException(ErrorCode errorCode) {
        super(errorCode);
    }
}
