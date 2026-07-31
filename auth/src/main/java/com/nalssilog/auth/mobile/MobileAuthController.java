package com.nalssilog.auth.mobile;

import com.nalssilog.auth.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth/mobile")
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;
    private final MobileDeviceInfoResolver deviceInfoResolver;
    private final AuthProperties properties;

    @GetMapping("/login/{provider}")
    public void login(
            @PathVariable String provider,
            @RequestParam(name = "redirect_uri") @NotBlank @Size(max = 500) String redirectUri,
            @RequestParam(name = "code_challenge") @NotBlank @Size(min = 43, max = 43) String codeChallenge,
            @RequestParam(name = "code_challenge_method") @NotBlank String codeChallengeMethod,
            @RequestParam @NotBlank @Size(min = 16, max = 256) String state,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(mobileAuthService.startLogin(
                provider,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                state));
    }

    @PostMapping("/token")
    public MobileTokenResponse token(
            @Valid @RequestBody MobileTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return MobileTokenResponse.from(
                mobileAuthService.exchange(
                        request.code(),
                        request.codeVerifier(),
                        request.redirectUri(),
                        deviceInfoResolver.resolve(request.device(), httpRequest)),
                properties);
    }

    @PostMapping("/refresh")
    public MobileRefreshResponse refresh(
            @Valid @RequestBody MobileRefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        return MobileRefreshResponse.from(
                mobileAuthService.refresh(
                        request.refreshToken(),
                        deviceInfoResolver.resolve(request.device(), httpRequest)),
                properties);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody MobileLogoutRequest request) {
        mobileAuthService.logout(request.refreshToken());
    }

    @PostMapping("/signup")
    public MobileTokenResponse signup(
            @Valid @RequestBody MobileSignupRequest request,
            HttpServletRequest httpRequest
    ) {
        return MobileTokenResponse.signup(
                mobileAuthService.signup(
                        request.signupTicket(),
                        request.agreedTerms(),
                        deviceInfoResolver.resolve(request.device(), httpRequest)),
                properties);
    }

    @PostMapping("/link/consent")
    public MobileAuthorizationResponse consentLink(
            @Valid @RequestBody MobileLinkConsentRequest request
    ) {
        return new MobileAuthorizationResponse(mobileAuthService.consentLink(
                request.linkTicket(),
                request.redirectUri(),
                request.codeChallenge(),
                request.codeChallengeMethod(),
                request.state()));
    }

    @PostMapping("/link/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelLink(@Valid @RequestBody MobileLinkCancelRequest request) {
        mobileAuthService.cancelLink(request.linkTicket());
    }

    @PostMapping("/link/social/{provider}")
    public MobileAuthorizationResponse startSettingsLink(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String provider,
            @Valid @RequestBody MobileLinkStartRequest request
    ) {
        return new MobileAuthorizationResponse(mobileAuthService.startSettingsLink(
                memberId,
                provider,
                request.redirectUri(),
                request.codeChallenge(),
                request.codeChallengeMethod(),
                request.state()));
    }
}
