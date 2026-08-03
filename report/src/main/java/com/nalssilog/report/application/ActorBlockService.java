package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.MemberVisibilityPolicy;
import com.nalssilog.report.api.dto.AuthorBlockResponse;
import com.nalssilog.report.api.dto.BlockedAuthorPageResponse;
import com.nalssilog.report.api.dto.BlockedAuthorPageResponse.Avatar;
import com.nalssilog.report.api.dto.BlockedAuthorPageResponse.BlockedAuthor;
import com.nalssilog.report.application.dto.AuthorInfo;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.client.MemberClient;
import com.nalssilog.report.domain.ActorBlock;
import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.repository.ActorBlockJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActorBlockService implements MemberVisibilityPolicy {

    private static final int MAX_BLOCKS = 200;
    private static final int MAX_PAGE_SIZE = 50;

    private final ActorBlockJpaRepository blockRepository;
    private final MemberClient memberClient;

    @Transactional
    public AuthorBlockResponse blockMember(Long memberId, ReportActor blocker) {
        ensureMemberBlocker(blocker);

        if (memberClient.findActiveAuthor(memberId).isEmpty()) {
            throw new NalssiLogException(ReportErrorCode.BLOCK_TARGET_MEMBER_NOT_FOUND);
        }

        return block(blocker, ReportActor.member(memberId));
    }

    private AuthorBlockResponse block(
            ReportActor blocker,
            ReportActor blocked
    ) {
        ensureMemberBlocker(blocker);

        if (sameActor(blocker, blocked)) {
            throw new NalssiLogException(ReportErrorCode.CANNOT_BLOCK_SELF);
        }

        Optional<ActorBlock> existing = find(blocker, blocked);

        if (existing.isPresent()) {
            return response(existing.get(), true);
        }

        if (blockRepository.countByBlockerTypeAndBlockerKey(blocker.type(), blocker.actorKey()) >= MAX_BLOCKS) {
            throw new NalssiLogException(ReportErrorCode.BLOCK_LIMIT_REACHED);
        }

        try {
            return response(blockRepository.saveAndFlush(
                    ActorBlock.create(blocker, blocked)), true);
        } catch (DataIntegrityViolationException exception) {
            return find(blocker, blocked)
                    .map(block -> response(block, true))
                    .orElseThrow(() -> exception);
        }
    }

    @Transactional
    public AuthorBlockResponse unblockMember(Long memberId, ReportActor blocker) {
        ensureMemberBlocker(blocker);
        Optional<ActorBlock> block = find(blocker, ReportActor.member(memberId));

        block.ifPresent(blockRepository::delete);

        return new AuthorBlockResponse(
                block.map(value -> String.valueOf(value.getId())).orElse(null), false);
    }

    @Transactional
    public AuthorBlockResponse unblock(Long blockId, ReportActor blocker) {
        ensureMemberBlocker(blocker);
        Optional<ActorBlock> found = blockRepository.findById(blockId)
                .filter(block -> block.getBlockerType() == blocker.type())
                .filter(block -> block.getBlockerKey().equals(blocker.actorKey()));

        found.ifPresent(blockRepository::delete);

        return new AuthorBlockResponse(found.map(block -> String.valueOf(block.getId())).orElse(null), false);
    }

    public BlockedAuthorPageResponse list(ReportActor blocker, int page, int size) {
        ensureMemberBlocker(blocker);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Page<ActorBlock> blocks = blockRepository.findAllByBlockerTypeAndBlockerKeyOrderByCreatedAtDesc(
                blocker.type(), blocker.actorKey(), PageRequest.of(safePage, safeSize));

        return new BlockedAuthorPageResponse(
                blocks.getContent().stream().map(this::toBlockedAuthor).toList(),
                blocks.getNumber(),
                blocks.getSize(),
                blocks.getTotalElements(),
                blocks.getTotalPages());
    }

    @Override
    public boolean canView(Long viewerMemberId, Long targetMemberId) {
        if (viewerMemberId == null
                || targetMemberId == null
                || viewerMemberId.equals(targetMemberId)) {
            return true;
        }

        ReportActor viewer = ReportActor.member(viewerMemberId);
        ReportActor target = ReportActor.member(targetMemberId);

        return find(viewer, target).isEmpty()
                && find(target, viewer).isEmpty();
    }

    private BlockedAuthor toBlockedAuthor(ActorBlock block) {
        Long memberId = Long.valueOf(block.getBlockedKey());
        AuthorInfo author = memberClient.findActiveAuthor(memberId).orElse(null);

        return new BlockedAuthor(
                String.valueOf(block.getId()),
                ActorType.MEMBER,
                String.valueOf(memberId),
                author == null ? "탈퇴한 이웃" : author.nickname(),
                author == null ? null : new Avatar(author.avatarType(), author.avatarValue()),
                block.getCreatedAt());
    }

    private Optional<ActorBlock> find(ReportActor blocker, ReportActor blocked) {
        return blockRepository.findByBlockerTypeAndBlockerKeyAndBlockedTypeAndBlockedKey(
                blocker.type(), blocker.actorKey(), blocked.type(), blocked.actorKey());
    }

    private void ensureMemberBlocker(ReportActor blocker) {
        if (blocker == null || blocker.type() != ActorType.MEMBER || blocker.memberId() == null) {
            throw new NalssiLogException(ReportErrorCode.BLOCK_MEMBER_REQUIRED);
        }
    }

    private boolean sameActor(ReportActor first, ReportActor second) {
        return first.type() == second.type() && first.actorKey().equals(second.actorKey());
    }

    private AuthorBlockResponse response(ActorBlock block, boolean blocked) {
        return new AuthorBlockResponse(String.valueOf(block.getId()), blocked);
    }
}
