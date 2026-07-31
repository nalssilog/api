package com.nalssilog.auth.security;

import com.nalssilog.auth.core.AuthErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CredentialAuthenticationException extends AuthenticationException {

    private final AuthErrorCode errorCode;
    private final boolean bearer;

    public CredentialAuthenticationException(AuthErrorCode errorCode) {
        this(errorCode, true);
    }

    public CredentialAuthenticationException(
            AuthErrorCode errorCode,
            boolean bearer
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.bearer = bearer;
    }
}
