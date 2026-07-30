package com.nalssilog.auth.mobile;

import com.nalssilog.auth.core.AuthService.SignupResult;
import com.nalssilog.auth.core.AuthService;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService.ExchangeResult;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.member.application.dto.TermsAgreement;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MobileAuthService {

    private final MobileOAuthService mobileOAuthService;
    private final AuthService authService;

    public String startLogin(
            String provider,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String state
    ) {

        return mobileOAuthService.startLogin(
                provider,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                state);
    }

    public ExchangeResult exchange(
            String code,
            String verifier,
            String redirectUri,
            DeviceInfo device
    ) {

        return mobileOAuthService.exchange(code, verifier, redirectUri, device);
    }

    public TokenPair refresh(String refreshToken, DeviceInfo device) {

        return authService.refreshMobile(refreshToken, device);
    }

    public void logout(String refreshToken) {
        authService.logout(Optional.ofNullable(refreshToken));
    }

    public SignupResult signup(
            String ticketId,
            List<TermsAgreement> agreedTerms,
            DeviceInfo device
    ) {
        mobileOAuthService.mobileSignupTicket(ticketId);

        return authService.signupMobile(ticketId, agreedTerms, device);
    }

    public String consentLink(
            String linkTicket,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String state
    ) {

        return mobileOAuthService.startLinkReauthentication(
                linkTicket,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                state);
    }

    public void cancelLink(String linkTicket) {
        mobileOAuthService.cancelLink(linkTicket);
    }

    public String startSettingsLink(
            Long memberId,
            String provider,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String state
    ) {

        return mobileOAuthService.startSettingsLink(
                memberId,
                provider,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                state);
    }
}
