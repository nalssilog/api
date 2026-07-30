package com.nalssilog.app.api;

import com.nalssilog.auth.core.RefreshRejectedException;
import com.nalssilog.auth.web.AuthCookieManager;
import com.nalssilog.common.exception.ErrorCode;
import com.nalssilog.common.exception.ErrorResponse;
import com.nalssilog.common.exception.NalssiLogException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuthCookieManager authCookieManager;

    @ExceptionHandler(NalssiLogException.class)
    public ErrorResponse handleNalssiLogException(
            NalssiLogException exception,
            HttpServletResponse response
    ) {
        if (exception instanceof RefreshRejectedException) {
            authCookieManager.clearAuthCookies(response);
        }

        ErrorCode errorCode = exception.getErrorCode();

        response.setStatus(errorCode.getStatus().value());
        log.warn("NalssiLogException [{}] {} (status={})",
                errorCode.getCode(), exception.getMessage(), errorCode.getStatus().value());

        return new ErrorResponse(errorCode.getCode(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ErrorResponse handleRequestParameterValidation(Exception exception) {

        return new ErrorResponse(
                "VALIDATION_ERROR",
                "요청 파라미터가 올바르지 않습니다.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException exception) {

        return new ErrorResponse("INVALID_REQUEST", "요청 본문을 해석할 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException exception) {

        return new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpectedException(Exception exception) {
        log.error("Unexpected exception", exception);

        return new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다.");
    }
}
