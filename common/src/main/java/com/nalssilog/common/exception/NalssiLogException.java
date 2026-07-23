package com.nalssilog.common.exception;

import lombok.Getter;

@Getter
public class NalssiLogException extends RuntimeException {

    private final ErrorCode errorCode;

    public NalssiLogException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NalssiLogException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
