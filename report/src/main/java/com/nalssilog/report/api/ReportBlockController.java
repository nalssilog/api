package com.nalssilog.report.api;

import com.nalssilog.report.api.dto.AuthorBlockResponse;
import com.nalssilog.report.api.dto.BlockedAuthorPageResponse;
import com.nalssilog.report.application.ActorBlockService;
import com.nalssilog.report.application.dto.ReportActor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-blocks")
@RequiredArgsConstructor
public class ReportBlockController {

    private final ActorBlockService blockService;

    @PostMapping("/members/{memberId}")
    public AuthorBlockResponse blockMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal Long blockerMemberId
    ) {
        return blockService.blockMember(memberId, ReportActor.member(blockerMemberId));
    }

    @DeleteMapping("/members/{memberId}")
    public AuthorBlockResponse unblockMember(
            @PathVariable Long memberId,
            @AuthenticationPrincipal Long blockerMemberId
    ) {
        return blockService.unblockMember(memberId, ReportActor.member(blockerMemberId));
    }

    @GetMapping
    public BlockedAuthorPageResponse list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return blockService.list(ReportActor.member(memberId), page, size);
    }

    @DeleteMapping("/{blockId}")
    public AuthorBlockResponse unblock(
            @PathVariable Long blockId,
            @AuthenticationPrincipal Long memberId
    ) {
        return blockService.unblock(blockId, ReportActor.member(memberId));
    }
}
