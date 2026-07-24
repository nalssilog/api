package com.nalssilog.member.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import com.nalssilog.common.domain.Tsid;
import com.nalssilog.common.exception.NalssiLogException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    public static final int NAME_MAX_LENGTH = 30;
    public static final int NICKNAME_MAX_LENGTH = 10;
    public static final int NICKNAME_MIN_LENGTH = 2;
    public static final int NICKNAME_STORAGE_MAX_LENGTH = 20;
    public static final int AVATAR_VALUE_MAX_LENGTH = 500;
    /** 허용 문자: 한글(완성형)·영문 대소문자·숫자만. 공백·특수문자 불가. (길이는 @Size 로 별도 검증) */
    public static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]+$";

    /** 가입 시 무작위 부여하는 기본 프로필 프리셋. 실제 이미지는 프론트가 이 id 로 렌더한다. */
    private static final List<String> DEFAULT_AVATAR_PRESETS = List.of("avatar-01", "avatar-02", "avatar-03", "avatar-04");

    @Id
    @Tsid
    private Long id;

    @Column(name = "email", nullable = true, length = 255)
    private String email;

    @Column(name = "name", nullable = true, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "nickname", nullable = true, length = NICKNAME_STORAGE_MAX_LENGTH)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_type", nullable = false, length = 20)
    private AvatarType avatarType;

    @Column(name = "avatar_value", nullable = true, length = AVATAR_VALUE_MAX_LENGTH)
    private String avatarValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "withdrawn_at", nullable = true)
    private Instant withdrawnAt;

    /** 가입 확정 시 ACTIVE 회원을 처음 생성(가입 전엔 signup 티켓에만 존재). */
    public static Member register(String email, String name, String nickname) {
        Member member = new Member();
        member.email = email;
        member.name = name;
        member.nickname = nickname;
        member.avatarType = AvatarType.PRESET;
        member.avatarValue = DEFAULT_AVATAR_PRESETS.get(ThreadLocalRandom.current().nextInt(DEFAULT_AVATAR_PRESETS.size()));
        member.status = MemberStatus.ACTIVE;

        return member;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeAvatar(AvatarType avatarType, String avatarValue) {
        if (avatarType != AvatarType.DEFAULT && (avatarValue == null || avatarValue.isBlank())) {
            throw new NalssiLogException(MemberErrorCode.INVALID_AVATAR);
        }

        this.avatarType = avatarType;
        this.avatarValue = avatarType == AvatarType.DEFAULT ? null : avatarValue;
    }

    /** 탈퇴: WITHDRAWN + 개인정보 익명화(닉네임도 null 로 비워 재사용 허용). 제보는 별도 익명화. */
    public void withdraw() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new NalssiLogException(MemberErrorCode.ALREADY_WITHDRAWN);
        }
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
        this.email = null;
        this.name = null;
        this.nickname = null;
        this.avatarType = AvatarType.DEFAULT;
        this.avatarValue = null;
    }
}
