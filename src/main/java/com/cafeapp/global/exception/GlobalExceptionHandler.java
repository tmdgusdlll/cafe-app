package com.cafeapp.global.exception;

import com.cafeapp.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    // 서비스 커스텀 예외
    @ExceptionHandler(CafeException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoffeeException(CafeException e) {
        log.error("CoffeeException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())  // ErrorCode의 HttpStatus 사용
                .body(ApiResponse.fail(
                        e.getErrorCode().getStatus().name(),
                        e.getErrorCode().getMessage()
                ));
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("INVALID_REQUEST", message));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage());
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
    }


}
