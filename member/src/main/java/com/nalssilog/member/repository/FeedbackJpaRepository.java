package com.nalssilog.member.repository;

import com.nalssilog.member.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link FeedbackRepository} 래퍼를 통해 사용한다.
 */
public interface FeedbackJpaRepository extends JpaRepository<Feedback, Long> {
}
