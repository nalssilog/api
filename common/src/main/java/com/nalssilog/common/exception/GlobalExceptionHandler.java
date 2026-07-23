package com.nalssilog.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NalssiLogException.class)
    public ResponseEntity<ErrorResponse> handleNalssiLogException(NalssiLogException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("NalssiLogException [{}] {} (status={})",
                errorCode.getCode(), e.getMessage(), errorCode.getStatus().value());

        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), e.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException e) {
        return new ErrorResponse("INVALID_REQUEST", "요청 본문을 해석할 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException e) {
        return new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpectedException(Exception e) {
        log.error("Unexpected exception", e);

        return new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다.");
    }
}
