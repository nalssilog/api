package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.oauth.apple.AppleOAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

@Component
public class MobileOAuthAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final MobileOAuthTransactionStore transactionStore;

    public MobileOAuthAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            MobileOAuthTransactionStore transactionStore
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
        this.transactionStore = transactionStore;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {

        return attachMobileTransaction(
                request,
                customizeProvider(delegate.resolve(request)));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String clientRegistrationId
    ) {

        return attachMobileTransaction(
                request,
                customizeProvider(
                        delegate.resolve(request, clientRegistrationId)));
    }

    private OAuth2AuthorizationRequest customizeProvider(
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {

            return null;
        }

        String registrationId = authorizationRequest.getAttribute(
                OAuth2ParameterNames.REGISTRATION_ID);

        if (!AppleOAuthProperties.REGISTRATION_ID.equals(registrationId)) {

            return authorizationRequest;
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(parameters ->
                        parameters.put("response_mode", "form_post"))
                .build();
    }

    private OAuth2AuthorizationRequest attachMobileTransaction(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {

            return null;
        }

        String[] values = request.getParameterValues(
                MobileOAuthRequestAttributes.TRANSACTION_PARAMETER);

        if (values == null || values.length == 0) {

            return authorizationRequest;
        }
        if (values.length != 1 || values[0] == null || values[0].isBlank()) {
            throw invalidTransaction();
        }

        String transactionId = values[0];
        MobileOAuthTransaction transaction = transactionStore.find(transactionId)
                .orElseThrow(this::invalidTransaction);
        String registrationId = authorizationRequest.getAttribute(
                OAuth2ParameterNames.REGISTRATION_ID);

        if (registrationId == null
                || !transaction.provider().name().equalsIgnoreCase(registrationId)) {
            throw invalidTransaction();
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .attributes(attributes -> attributes.put(
                        MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE,
                        transactionId))
                .build();
    }

    private OAuth2AuthenticationException invalidTransaction() {

        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_mobile_transaction"),
                "Invalid mobile OAuth transaction");
    }
}
