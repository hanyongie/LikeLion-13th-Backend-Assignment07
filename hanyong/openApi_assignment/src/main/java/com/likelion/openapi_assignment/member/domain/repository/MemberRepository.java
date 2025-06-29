package com.likelion.openapi_assignment.member.domain.repository;

import com.likelion.openapi_assignment.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
