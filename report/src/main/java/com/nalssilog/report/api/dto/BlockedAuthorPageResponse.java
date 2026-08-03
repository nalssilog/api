package com.nalssilog.report.api.dto;

import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.report.domain.ActorType;
import java.time.Instant;
import java.util.List;

public record BlockedAuthorPageResponse(
        List<BlockedAuthor> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record BlockedAuthor(
            String blockId,
            ActorType type,
            String memberId,
            String nickname,
            Avatar avatar,
            Instant blockedAt
    ) {
    }

    /** PRESET은 프리셋 ID, CUSTOM은 공개 이미지 URL, DEFAULT는 value=null. */
    public record Avatar(AvatarType type, String value) {
    }
}
