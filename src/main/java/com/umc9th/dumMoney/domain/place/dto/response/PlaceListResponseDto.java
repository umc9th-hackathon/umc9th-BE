package com.umc9th.dumMoney.domain.place.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 목록 안에 들어갈 PlaceListResponseDto
public class PlaceListResponseDto {
    private Long id;
    private String name;
    private String category;    // "RESTAURANT" or "CAFE"
    private Double latitude;    // Entity의 lat
    private Double longitude;   // Entity의 lng
    private Double distance;    // 계산된 거리 (미터)
    private String address;     // roadAddress
    private String imageUrl;
    private String openingHours;
}
