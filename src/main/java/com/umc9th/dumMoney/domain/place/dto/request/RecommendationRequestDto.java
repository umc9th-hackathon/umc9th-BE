package com.umc9th.dumMoney.domain.place.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationRequestDto {
    private Long memberId;
    private String userPrompt;

    // 자연어 장소명 (우선순위 높음)
    private String startLocation;  // 예: "중앙대 정문", "강남역", "서울역"
    private String endLocation;    // 예: "신촌역", "홍대입구역", "선릉역"
    
    // 위도/경도 좌표 (startLocation/endLocation이 없을 때 사용, 또는 내부 처리용)
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
}