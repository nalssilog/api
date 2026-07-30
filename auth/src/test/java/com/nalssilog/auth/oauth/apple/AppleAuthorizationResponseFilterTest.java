package com.nalssilog.auth.oauth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class AppleAuthorizationResponseFilterTest {

    @Test
    void exposesFirstAuthorizationNameOnlyWhileCallbackIsProcessed() throws Exception {
        AppleAuthorizationUserContext context =
                new AppleAuthorizationUserContext();
        AppleAuthorizationResponseFilter filter =
                new AppleAuthorizationResponseFilter(new ObjectMapper(), context);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                AppleAuthorizationResponseFilter.CALLBACK_PATH);

        request.setParameter(
                "user",
                """
                        {
                          "name": {
                            "firstName": "Gil-dong",
                            "lastName": "Hong"
                          },
                          "email": "must-not-be-trusted@example.com"
                        }
                        """);

        AtomicReference<String> nameSeenDuringCallback =
                new AtomicReference<>();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (nestedRequest, nestedResponse) ->
                        nameSeenDuringCallback.set(
                                context.currentSocialName().orElse(null)));

        assertThat(nameSeenDuringCallback.get()).isEqualTo("Gil-dong Hong");
        assertThat(context.currentSocialName()).isEmpty();
    }
}
