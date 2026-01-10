package com.umc9th.dumMoney.domain.place.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 전체를 감싸는 PlaceSearchResponseDto
public class PlaceSearchResponseDto {
    private int count;
    private List<PlaceListResponseDto> places;
}
