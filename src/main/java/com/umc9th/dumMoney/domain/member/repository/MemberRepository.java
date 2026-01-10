package com.umc9th.dumMoney.domain.member.repository;

import com.umc9th.dumMoney.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
