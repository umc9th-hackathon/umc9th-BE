package com.umc9th.dumMoney.domain.member.dto;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResponse {
    private Long memberId;
    private Category category;
    private Integer minBudget;
    private Integer maxBudget;
    private Integer distance; // meters (300, 500, 1000, 1500)

    public static PreferenceResponse from(Member member) {
        return PreferenceResponse.builder()
                .memberId(member.getMemberId())
                .category(member.getTargetCategory())
                .minBudget(member.getMinBudget())
                .maxBudget(member.getMaxBudget())
                .distance(member.getSearchRadius())
                .build();
    }
}
