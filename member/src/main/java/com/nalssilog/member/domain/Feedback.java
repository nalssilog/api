package com.nalssilog.member.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 피드백. 로그인 회원(authorMemberId) 또는 비로그인(null) 모두 제출할 수 있다. 작성자는 내용만 남긴다.
 * 작성자 회원은 다른 모듈이 아닌 member 모듈 내부 참조지만, 연관관계 없이 ID 로만 보관한다(선택적 익명).
 */
@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    public static final int CONTENT_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_member_id", nullable = true)
    private Long authorMemberId;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    public static Feedback create(Long authorMemberId, String content) {
        Feedback feedback = new Feedback();

        feedback.authorMemberId = authorMemberId;
        feedback.content = content;

        return feedback;
    }
}
