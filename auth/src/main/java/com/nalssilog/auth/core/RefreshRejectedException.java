package com.nalssilog.auth.core;

import com.nalssilog.common.exception.NalssiLogException;

/**
 * refresh 실패로 브라우저의 인증 쿠키까지 폐기해야 하는 경우를 HTTP 계층에 전달한다.
 */
public final class RefreshRejectedException extends NalssiLogException {

    public RefreshRejectedException(NalssiLogException cause) {
        super(cause.getErrorCode(), cause.getMessage());
        initCause(cause);
    }
}
