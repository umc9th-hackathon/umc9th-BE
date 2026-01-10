package com.umc9th.dumMoney.domain.member.dto;

import com.umc9th.dumMoney.domain.member.enums.Category;
import com.umc9th.dumMoney.domain.place.entity.PlaceCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateDto {
    private Integer minBudget;
    private Integer maxBudget;
    private Integer radius;
    private Category category;
}
