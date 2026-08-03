package com.nalssilog.member.repository;

import com.nalssilog.member.domain.MemberRoleChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRoleChangeJpaRepository extends JpaRepository<MemberRoleChange, Long> {

    Page<MemberRoleChange> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
