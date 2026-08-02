package com.nalssilog.member.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_role_change",
        indexes = {
                @Index(name = "idx_member_role_change_target", columnList = "target_member_id, created_at"),
                @Index(name = "idx_member_role_change_admin", columnList = "changed_by_member_id, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRoleChange extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Column(name = "changed_by_member_id", nullable = false)
    private Long changedByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_role", nullable = false, length = 20)
    private MemberRole previousRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", nullable = false, length = 20)
    private MemberRole newRole;

    public static MemberRoleChange record(
            Long targetMemberId,
            Long changedByMemberId,
            MemberRole previousRole,
            MemberRole newRole
    ) {
        MemberRoleChange change = new MemberRoleChange();

        change.targetMemberId = targetMemberId;
        change.changedByMemberId = changedByMemberId;
        change.previousRole = previousRole;
        change.newRole = newRole;

        return change;
    }
}
