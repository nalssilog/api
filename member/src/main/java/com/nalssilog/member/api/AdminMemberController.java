package com.nalssilog.member.api;

import com.nalssilog.member.api.dto.AdminMemberRoleResponse;
import com.nalssilog.member.api.dto.UpdateMemberRoleRequest;
import com.nalssilog.member.api.dto.AdminMemberRoleChangePageResponse;
import com.nalssilog.member.application.MemberAccountService;
import com.nalssilog.member.repository.MemberRoleChangeJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberAccountService memberAccountService;
    private final MemberRoleChangeJpaRepository roleChangeRepository;

    @PatchMapping("/{memberId}/role")
    public AdminMemberRoleResponse changeRole(
            @PathVariable Long memberId,
            @AuthenticationPrincipal Long adminMemberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return AdminMemberRoleResponse.from(
                memberAccountService.changeRole(memberId, request.role(), adminMemberId));
    }

    @GetMapping("/role-changes")
    public AdminMemberRoleChangePageResponse roleChanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return AdminMemberRoleChangePageResponse.from(
                roleChangeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(
                        Math.max(page, 0), Math.max(1, Math.min(size, 100)))));
    }
}
