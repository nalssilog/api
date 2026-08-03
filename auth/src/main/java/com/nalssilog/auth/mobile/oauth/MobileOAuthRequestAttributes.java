package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.ticket.AuthChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

public final class MobileOAuthRequestAttributes {

    public static final String TRANSACTION_PARAMETER = "mobile_transaction";
    public static final String AUTHORIZATION_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".authorizationTransaction";
    public static final String AUTHORIZATION_CHANNEL_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".authorizationChannel";
    private static final String REQUEST_TRANSACTION_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".requestTransaction";
    private static final String REQUEST_CHANNEL_ATTRIBUTE =
            MobileOAuthRequestAttributes.class.getName() + ".requestChannel";
    private static final String MOBILE_STATE_PREFIX = "m.";
    private static final String WEB_STATE_PREFIX = "w.";
    private static final Pattern MOBILE_STATE_PATTERN =
            Pattern.compile("^m\\.([A-Za-z0-9_-]{43})$");
    private static final Pattern TRANSACTION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private MobileOAuthRequestAttributes() {
    }

    public static String mobileState(String transactionId) {
        if (transactionId == null
                || !TRANSACTION_ID_PATTERN.matcher(transactionId).matches()) {
            throw new IllegalArgumentException("Invalid mobile OAuth transaction id");
        }

        return MOBILE_STATE_PREFIX + transactionId;
    }

    public static String webState(String generatedState) {
        if (generatedState == null || generatedState.isBlank()) {
            throw new IllegalArgumentException("OAuth state must not be blank");
        }

        return WEB_STATE_PREFIX + generatedState;
    }

    public static void expose(
            HttpServletRequest request,
            AuthChannel channel,
            String transactionId
    ) {
        request.setAttribute(REQUEST_CHANNEL_ATTRIBUTE, channel);

        if (transactionId != null && !transactionId.isBlank()) {
            request.setAttribute(REQUEST_TRANSACTION_ATTRIBUTE, transactionId);
        }
    }

    public static Optional<AuthChannel> channel(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_CHANNEL_ATTRIBUTE);

        if (value instanceof AuthChannel channel) {
            return Optional.of(channel);
        }

        String state = request.getParameter(OAuth2ParameterNames.STATE);

        if (state != null && state.startsWith(MOBILE_STATE_PREFIX)) {
            return Optional.of(AuthChannel.MOBILE);
        }

        if (state != null && state.startsWith(WEB_STATE_PREFIX)) {
            return Optional.of(AuthChannel.WEB);
        }

        return Optional.empty();
    }

    public static Optional<String> transactionId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_TRANSACTION_ATTRIBUTE);

        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(text);
        }

        return transactionIdFromState(request.getParameter(OAuth2ParameterNames.STATE));
    }

    public static Optional<AuthChannel> channelFromState(String state) {
        if (state != null && state.startsWith(MOBILE_STATE_PREFIX)) {
            return Optional.of(AuthChannel.MOBILE);
        }

        if (state != null && state.startsWith(WEB_STATE_PREFIX)) {
            return Optional.of(AuthChannel.WEB);
        }

        return Optional.empty();
    }

    public static Optional<String> transactionIdFromState(String state) {
        if (state == null) {
            return Optional.empty();
        }

        Matcher matcher = MOBILE_STATE_PATTERN.matcher(state);

        return matcher.matches()
                ? Optional.of(matcher.group(1))
                : Optional.empty();
    }
}
