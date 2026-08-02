package com.nalssilog.report.api;

import com.nalssilog.report.api.dto.PresignRequest;
import com.nalssilog.report.api.dto.PresignResponse;
import com.nalssilog.report.application.ReportImageService;
import com.nalssilog.report.application.ActorRestrictionService;
import com.nalssilog.report.application.ReportRateLimiter;
import com.nalssilog.report.application.dto.ImageUploadSpec;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.config.ReportActorResolver;
import com.nalssilog.report.config.ReportClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 제보 이미지 presigned 업로드 발급. 회원·익명 모두 제보에 사진을 붙일 수 있어 permitAll(POST /api/reports/**).
 * 상태변경이므로 X-XSRF-TOKEN 헤더 필요.
 */
@RestController
@RequestMapping("/api/reports/images")
@RequiredArgsConstructor
public class ReportImageController {

    private final ReportImageService reportImageService;
    private final ReportActorResolver actorResolver;
    private final ReportClientIpResolver clientIpResolver;
    private final ReportRateLimiter rateLimiter;
    private final ActorRestrictionService restrictionService;

    @PostMapping("/presign")
    public PresignResponse presign(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PresignRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        ReportActor actor = actorResolver.resolveForWrite(memberId, httpRequest, httpResponse);

        restrictionService.ensureCanPost(actor);
        rateLimiter.checkPresign(actor, clientIpResolver.resolve(httpRequest));

        List<ImageUploadSpec> specs = request.images().stream()
                .map(image -> new ImageUploadSpec(image.contentType(), image.size()))
                .toList();

        return PresignResponse.from(reportImageService.presign(specs));
    }
}
