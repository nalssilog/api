package com.nalssilog.auth.mobile.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class MobileOAuthRequestAttributes {

    public static final String TRANSACTION_PARAMETER = "mobile_transaction";
    public static final String AUTHORIZATION_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".authorizationTransaction";
    private static final String REQUEST_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".requestTransaction";

    private MobileOAuthRequestAttributes() {
    }

    public static void expose(HttpServletRequest request, String transactionId) {
        request.setAttribute(REQUEST_ATTRIBUTE, transactionId);
    }

    public static Optional<String> transactionId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);

        return value instanceof String text && !text.isBlank()
                ? Optional.of(text)
                : Optional.empty();
    }
}
