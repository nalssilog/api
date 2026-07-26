package com.nalssilog.auth.api;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nalssilog.auth.api.dto.LinkConsentResponse;
import com.nalssilog.auth.api.dto.MeResponse;
import com.nalssilog.auth.api.dto.SessionResponse;
import com.nalssilog.auth.api.dto.SignupRequest;
import com.nalssilog.auth.application.AuthService;
import com.nalssilog.auth.application.AuthService.SignupResult;
import com.nalssilog.auth.application.AuthService.SocialLinkStart;
import com.nalssilog.auth.application.TokenPair;
import com.nalssilog.auth.config.AuthCookieManager;
import com.nalssilog.auth.config.DeviceInfoResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthCookieManager cookieManager;
	private final DeviceInfoResolver deviceInfoResolver;

	/** 소셜 로그인 진입. 내부 Spring OAuth 경로를 은닉하고 302 시킨다. */
	@GetMapping("/login/{provider}")
	public void login(
		@PathVariable String provider,
		HttpServletResponse response
	) throws IOException {
		response.sendRedirect(authService.oauthAuthorizationUrl(provider));
	}

	/** 인증 상태 조회(stateless — AT/티켓 쿠키로 판단). */
	@GetMapping("/me")
	public MeResponse me(
		@AuthenticationPrincipal Long memberId,
		HttpServletRequest request
	) {
		boolean hasAuthenticationCookie = cookieManager.readAccessToken(request).isPresent()
			|| cookieManager.readRefreshToken(request).isPresent();

		return MeResponse.from(authService.me(
			memberId,
			cookieManager.readSignupTicket(request),
			cookieManager.readLinkTicket(request),
			hasAuthenticationCookie));
	}

	/** 회원가입 확정. OAuth 정보는 signup 티켓에서 읽고, Member 는 여기서 처음 생성된다. */
	@PostMapping("/signup")
	public MeResponse signup(
		@Valid @RequestBody SignupRequest request,
		HttpServletRequest httpRequest,
		HttpServletResponse response
	) {
		SignupResult result = authService.signup(cookieManager.readSignupTicket(httpRequest).orElse(null),
			request.agreedTerms(), deviceInfoResolver.resolve(httpRequest));
		TokenPair tokens = result.tokens();

		cookieManager.addAuthCookies(response, tokens.accessToken(), tokens.refreshToken(),
			tokens.refreshTokenMaxAge());
		cookieManager.clearSignupTicketCookie(response);

		return MeResponse.authenticated(result.member());
	}

	@PostMapping("/refresh")
	public void refresh(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		TokenPair tokens = authService.refresh(
			cookieManager.readRefreshToken(request).orElse(null),
			deviceInfoResolver.resolve(request));

		cookieManager.addAuthCookies(
			response, tokens.accessToken(), tokens.refreshToken(), tokens.refreshTokenMaxAge());
	}

	@PostMapping("/logout")
	public void logout(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		authService.logout(cookieManager.readRefreshToken(request));
		cookieManager.clearAuthCookies(response);
	}

	/** 회원 탈퇴: 익명화(member) + 제보 익명화(event) + 전 기기 세션 만료 + 쿠키 정리. 세션·쿠키 때문에 auth 소유. */
	@DeleteMapping("/withdraw")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(
		@AuthenticationPrincipal Long memberId,
		HttpServletResponse response
	) {
		authService.withdraw(memberId);
		cookieManager.clearAuthCookies(response);
	}

	/** 로그인된 기기 목록(current=이 기기). */
	@GetMapping("/sessions")
	public List<SessionResponse> sessions(
		@AuthenticationPrincipal Long memberId,
		HttpServletRequest request
	) {
		return authService.sessions(memberId, cookieManager.readRefreshToken(request)).stream()
			.map(SessionResponse::from)
			.toList();
	}

	/** 특정 기기 로그아웃. 대상이 현재 기기면 인증 쿠키까지 정리한다. */
	@DeleteMapping("/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeSession(
		@AuthenticationPrincipal Long memberId,
		@PathVariable String sessionId,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		boolean revokedCurrent = authService.revokeSession(
			memberId, sessionId, cookieManager.readRefreshToken(request));

		if (revokedCurrent) {
			cookieManager.clearAuthCookies(response);
		}
	}

	/** 설정에서 소셜 추가 연동 시작. intent 세팅 후 OAuth 진입 URL 반환(실제 연동은 성공 핸들러). */
	@PostMapping("/link/social/{provider}")
	public LinkConsentResponse startSocialLink(
		@AuthenticationPrincipal Long memberId,
		@PathVariable String provider,
		HttpServletResponse response
	) {
		SocialLinkStart link = authService.startSocialLink(memberId, provider);
		cookieManager.addLinkIntentCookie(response, link.intentId());

		return new LinkConsentResponse(link.authorizationUrl());
	}

	/** 로그인-시점 연동 동의 + 기존 수단 재인증 URL 반환. */
	@PostMapping("/link/consent")
	public LinkConsentResponse consentLink(HttpServletRequest request) {
		return new LinkConsentResponse(authService.consentLink(
			cookieManager.readLinkTicket(request).orElse(null)));
	}

	@GetMapping("/link/reauth/{provider}")
	public void linkReauth(
		@PathVariable String provider,
		HttpServletResponse response
	) throws IOException {
		response.sendRedirect(authService.oauthAuthorizationUrl(provider));
	}

	@PostMapping("/link/cancel")
	public void cancelLink(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		authService.cancelLink(cookieManager.readLinkTicket(request));
		cookieManager.clearLinkTicketCookie(response);
	}
}
