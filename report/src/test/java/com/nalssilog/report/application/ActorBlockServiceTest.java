package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.report.application.dto.AuthorInfo;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.client.MemberClient;
import com.nalssilog.report.domain.ActorBlock;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.repository.ActorBlockJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@SuppressWarnings("java:S5960")
class ActorBlockServiceTest {

    private final ActorBlockJpaRepository blockRepository = mock(ActorBlockJpaRepository.class);
    private final MemberClient memberClient = mock(MemberClient.class);
    private final ActorBlockService service = new ActorBlockService(
            blockRepository, memberClient);

    @Test
    void cannotBlockSelf() {
        when(memberClient.findActiveAuthor(7L)).thenReturn(Optional.of(
                new AuthorInfo(7L, "이웃", AvatarType.DEFAULT, null)));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.blockMember(7L, ReportActor.member(7L)));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.CANNOT_BLOCK_SELF);
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void guestCannotBlockMember() {
        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.blockMember(7L, ReportActor.anonymous("viewer")));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.BLOCK_MEMBER_REQUIRED);
        verify(memberClient, never()).findActiveAuthor(any());
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void memberCanBlockMemberFromProfile() {
        when(memberClient.findActiveAuthor(7L)).thenReturn(Optional.of(
                new AuthorInfo(7L, "이웃", AvatarType.PRESET, "avatar-01")));
        when(blockRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.blockMember(7L, ReportActor.member(8L)).blocked()).isTrue();

        verify(blockRepository).saveAndFlush(any(ActorBlock.class));
    }

    @Test
    void missingOrInactiveMemberCannotBeBlockedByMemberId() {
        when(memberClient.findActiveAuthor(7L)).thenReturn(Optional.empty());

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.blockMember(7L, ReportActor.member(8L)));

        assertThat(exception.getErrorCode())
                .isEqualTo(ReportErrorCode.BLOCK_TARGET_MEMBER_NOT_FOUND);
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void blockListIncludesMemberAvatarContract() {
        ActorBlock block = ActorBlock.create(
                ReportActor.member(8L), ReportActor.member(7L));

        when(blockRepository.findAllByBlockerTypeAndBlockerKeyOrderByCreatedAtDesc(
                any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(block)));
        when(memberClient.findActiveAuthor(7L)).thenReturn(Optional.of(
                new AuthorInfo(7L, "이웃", AvatarType.PRESET, "avatar-01")));

        var item = service.list(ReportActor.member(8L), 0, 20).items().getFirst();

        assertThat(item.nickname()).isEqualTo("이웃");
        assertThat(item.avatar().type()).isEqualTo(AvatarType.PRESET);
        assertThat(item.avatar().value()).isEqualTo("avatar-01");
    }

}
