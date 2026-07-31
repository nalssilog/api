package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.mobile.guest.MobileGuestCredentialService.IssuedGuestCredential;

public record GuestCredentialResponse(String guestToken, long expiresIn) {

    public static GuestCredentialResponse from(IssuedGuestCredential credential) {
        return new GuestCredentialResponse(
                credential.token(),
                credential.expiresIn().toSeconds());
    }
}
