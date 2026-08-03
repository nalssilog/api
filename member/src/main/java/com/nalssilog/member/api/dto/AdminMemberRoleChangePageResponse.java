package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.MemberRole;
import com.nalssilog.member.domain.MemberRoleChange;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record AdminMemberRoleChangePageResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Item(
            String id,
            String targetMemberId,
            String changedByMemberId,
            MemberRole previousRole,
            MemberRole newRole,
            Instant createdAt
    ) {
    }

    public static AdminMemberRoleChangePageResponse from(Page<MemberRoleChange> changes) {
        List<Item> items = changes.getContent().stream()
                .map(change -> new Item(
                        String.valueOf(change.getId()),
                        String.valueOf(change.getTargetMemberId()),
                        String.valueOf(change.getChangedByMemberId()),
                        change.getPreviousRole(),
                        change.getNewRole(),
                        change.getCreatedAt()))
                .toList();

        return new AdminMemberRoleChangePageResponse(
                items,
                changes.getNumber(),
                changes.getSize(),
                changes.getTotalElements(),
                changes.getTotalPages());
    }
}
