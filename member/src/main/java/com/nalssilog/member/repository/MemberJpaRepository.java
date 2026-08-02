package com.nalssilog.member.repository;

import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.MemberRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link MemberRepository} 래퍼를 통해 사용한다.
 */
public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    long countByRole(MemberRole role);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    Optional<Member> findFirstByEmailAndStatusNot(String email, MemberStatus status);
}
