package com.nalssilog.member.api;

import com.nalssilog.member.api.dto.AvatarPresignRequest;
import com.nalssilog.member.api.dto.AvatarPresignResponse;
import com.nalssilog.member.api.dto.ChangeAvatarRequest;
import com.nalssilog.member.api.dto.ChangeNicknameRequest;
import com.nalssilog.member.api.dto.MemberMeResponse;
import com.nalssilog.member.api.dto.MemberPublicProfileResponse;
import com.nalssilog.member.api.dto.NicknameAvailabilityResponse;
import com.nalssilog.member.api.dto.SocialAccountResponse;
import com.nalssilog.member.application.MemberProfileService;
import com.nalssilog.member.domain.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberProfileService memberProfileService;

    @GetMapping("/nickname/availability")
    public NicknameAvailabilityResponse checkNickname(@RequestParam @NotBlank String nickname) {
        return new NicknameAvailabilityResponse(memberProfileService.isNicknameAvailable(nickname));
    }

    @GetMapping("/me")
    public MemberMeResponse me(@AuthenticationPrincipal Long memberId) {
        return MemberMeResponse.from(memberProfileService.getMe(memberId));
    }

    @GetMapping("/{id}")
    public MemberPublicProfileResponse publicProfile(@PathVariable Long id) {
        return MemberPublicProfileResponse.from(memberProfileService.getPublicProfile(id));
    }

    @PatchMapping("/me/nickname")
    public MemberMeResponse changeNickname(@AuthenticationPrincipal Long memberId,
                                           @Valid @RequestBody ChangeNicknameRequest request) {
        return MemberMeResponse.from(memberProfileService.changeNickname(memberId, request.nickname()));
    }

    @PatchMapping("/me/avatar")
    public MemberMeResponse changeAvatar(@AuthenticationPrincipal Long memberId,
                                         @Valid @RequestBody ChangeAvatarRequest request) {
        return MemberMeResponse.from(
                memberProfileService.changeAvatar(memberId, request.type(), request.value()));
    }

    /**
     * 커스텀 아바타 업로드용 presigned URL 발급. 업로드 후 PATCH /me/avatar 로 {type:CUSTOM, value:storageKey} 전송.
     */
    @PostMapping("/me/avatar/presign")
    public AvatarPresignResponse presignAvatar(@AuthenticationPrincipal Long memberId,
                                               @Valid @RequestBody AvatarPresignRequest request) {
        return AvatarPresignResponse.from(
                memberProfileService.presignAvatar(memberId, request.contentType(), request.size()));
    }

    @GetMapping("/me/social-accounts")
    public List<SocialAccountResponse> socialAccounts(@AuthenticationPrincipal Long memberId) {
        return memberProfileService.getSocialAccounts(memberId).stream()
                .map(SocialAccountResponse::from)
                .toList();
    }

    @DeleteMapping("/me/social-accounts/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkSocial(@AuthenticationPrincipal Long memberId, @PathVariable String provider) {
        memberProfileService.unlinkSocial(memberId, Provider.from(provider));
    }
}
