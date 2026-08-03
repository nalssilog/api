package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.ticket.AuthChannel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class MobileOAuthAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String AUTHORIZATION_REQUESTS_ATTRIBUTE =
            MobileOAuthAuthorizationRequestRepository.class.getName()
                    + ".AUTHORIZATION_REQUESTS";
    private static final int MAX_AUTHORIZATION_REQUESTS = 16;

    private final HttpSessionOAuth2AuthorizationRequestRepository legacyDelegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();
    private final MobileOAuthAuthorizationRequestStore mobileStore;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");

        String state = request.getParameter(OAuth2ParameterNames.STATE);
        Optional<String> mobileTransaction =
                MobileOAuthRequestAttributes.transactionIdFromState(state);

        if (mobileTransaction.isPresent()) {
            Optional<OAuth2AuthorizationRequest> mobileRequest =
                    mobileStore.find(mobileTransaction.get());

            if (mobileRequest.isPresent()) {
                return mobileRequest.get();
            }
        }

        HttpSession session = request.getSession(false);

        if (state == null || session == null) {
            return null;
        }

        synchronized (session) {
            return authorizationRequests(session).get(state);
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);

            return;
        }

        String state = authorizationRequest.getState();

        Assert.hasText(state, "authorizationRequest.state cannot be empty");

        Optional<String> mobileTransaction =
                authorizationTransaction(authorizationRequest)
                        .or(() -> MobileOAuthRequestAttributes.transactionIdFromState(state));

        if (mobileTransaction.isPresent()) {
            mobileStore.save(mobileTransaction.get(), authorizationRequest);

            return;
        }

        HttpSession session = request.getSession();

        synchronized (session) {
            LinkedHashMap<String, OAuth2AuthorizationRequest> updated =
                    new LinkedHashMap<>(authorizationRequests(session));

            updated.remove(state);
            updated.put(state, authorizationRequest);

            while (updated.size() > MAX_AUTHORIZATION_REQUESTS) {
                updated.remove(updated.keySet().iterator().next());
            }

            session.setAttribute(AUTHORIZATION_REQUESTS_ATTRIBUTE, updated);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        String state = request.getParameter(OAuth2ParameterNames.STATE);
        Optional<String> mobileTransaction =
                MobileOAuthRequestAttributes.transactionIdFromState(state);
        OAuth2AuthorizationRequest mobileRequest = mobileTransaction
                .flatMap(mobileStore::take)
                .orElse(null);
        OAuth2AuthorizationRequest sessionRequest = removeByState(request, state);
        OAuth2AuthorizationRequest authorizationRequest = mobileRequest != null
                ? mobileRequest
                : sessionRequest;

        if (authorizationRequest == null) {
            authorizationRequest = legacyDelegate.removeAuthorizationRequest(request, response);
        }

        exposeFlow(request, state, authorizationRequest);

        return authorizationRequest;
    }

    private OAuth2AuthorizationRequest removeByState(
            HttpServletRequest request,
            String state
    ) {
        HttpSession session = request.getSession(false);

        if (state == null || session == null) {
            return null;
        }

        synchronized (session) {
            LinkedHashMap<String, OAuth2AuthorizationRequest> updated =
                    new LinkedHashMap<>(authorizationRequests(session));
            OAuth2AuthorizationRequest authorizationRequest = updated.remove(state);

            if (authorizationRequest == null) {
                return null;
            }

            if (updated.isEmpty()) {
                session.removeAttribute(AUTHORIZATION_REQUESTS_ATTRIBUTE);
            } else {
                session.setAttribute(AUTHORIZATION_REQUESTS_ATTRIBUTE, updated);
            }

            return authorizationRequest;
        }
    }

    private void exposeFlow(
            HttpServletRequest request,
            String state,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        Optional<AuthChannel> channel = authorizationChannel(authorizationRequest)
                .or(() -> MobileOAuthRequestAttributes.channelFromState(state));
        String transactionId = authorizationTransaction(authorizationRequest)
                .or(() -> MobileOAuthRequestAttributes.transactionIdFromState(state))
                .orElse(null);

        if (channel.isEmpty() && transactionId != null) {
            channel = Optional.of(AuthChannel.MOBILE);
        }

        channel.ifPresent(authChannel -> MobileOAuthRequestAttributes.expose(
                request,
                authChannel,
                transactionId));
    }

    private Optional<AuthChannel> authorizationChannel(
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return Optional.empty();
        }

        Object value = authorizationRequest.getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE);

        if (value instanceof AuthChannel channel) {
            return Optional.of(channel);
        }

        if (value instanceof String text) {
            try {
                return Optional.of(AuthChannel.valueOf(text));
            } catch (IllegalArgumentException _) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private Optional<String> authorizationTransaction(
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return Optional.empty();
        }

        String transactionId = authorizationRequest.getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE);

        return transactionId == null || transactionId.isBlank()
                ? Optional.empty()
                : Optional.of(transactionId);
    }

    private Map<String, OAuth2AuthorizationRequest> authorizationRequests(
            HttpSession session
    ) {
        Object value = session.getAttribute(AUTHORIZATION_REQUESTS_ATTRIBUTE);

        if (!(value instanceof Map<?, ?> stored)) {
            return Map.of();
        }

        LinkedHashMap<String, OAuth2AuthorizationRequest> requests =
                new LinkedHashMap<>();

        stored.forEach((state, authorizationRequest) -> {
            if (state instanceof String text
                    && authorizationRequest instanceof OAuth2AuthorizationRequest request) {
                requests.put(text, request);
            }
        });

        return requests;
    }
}
