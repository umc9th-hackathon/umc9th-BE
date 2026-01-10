package com.umc9th.dumMoney.domain.place.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationRequestDto {
    private Long memberId;
    private String userPrompt;
}