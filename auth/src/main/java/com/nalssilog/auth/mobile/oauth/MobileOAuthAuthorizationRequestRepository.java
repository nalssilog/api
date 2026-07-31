package com.nalssilog.auth.mobile.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class MobileOAuthAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest =
                delegate.removeAuthorizationRequest(request, response);

        if (authorizationRequest != null) {
            String transactionId = authorizationRequest.getAttribute(
                    MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE);

            if (transactionId != null && !transactionId.isBlank()) {
                MobileOAuthRequestAttributes.expose(request, transactionId);
            }
        }

        return authorizationRequest;
    }
}
