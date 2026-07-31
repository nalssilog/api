package com.nalssilog.auth.oauth.apple;

import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

@Configuration
public class AppleOAuthConfig {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            authorizationCodeTokenResponseClient(
                    AppleClientSecretGenerator clientSecretGenerator
            ) {
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        DefaultOAuth2TokenRequestParametersConverter<OAuth2AuthorizationCodeGrantRequest>
                defaults = new DefaultOAuth2TokenRequestParametersConverter<>();

        client.setParametersConverter(request -> {
            MultiValueMap<String, String> parameters =
                    Objects.requireNonNull(defaults.convert(request));

            if (AppleOAuthProperties.REGISTRATION_ID.equals(
                    request.getClientRegistration().getRegistrationId())) {
                parameters.set(
                        OAuth2ParameterNames.CLIENT_SECRET,
                        clientSecretGenerator.generate(
                                request.getClientRegistration().getClientId()));
            }

            return parameters;
        });

        return client;
    }
}
