package com.umc9th.dumMoney.domain.member.dto;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.enums.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreferenceResponse {
    private Long memberId;
    private Category category;
    private Integer budget;
    private Integer distance; // meters (300, 500, 1000)

    public static PreferenceResponse from(Member member) {
        return PreferenceResponse.builder()
                .memberId(member.getMemberId())
                .category(member.getTargetCategory())
                .budget(member.getCurrentBudget())
                .distance(member.getSearchRadius())
                .build();
    }
}
