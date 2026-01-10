package com.umc9th.dumMoney.domain.place.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationRequestDto {
    private Long memberId;
    private String userPrompt;

    //출발지 좌표
    private Double startLat;
    private Double startLng;

    //도착지 좌표
    private Double endLat;
    private Double endLng;
}