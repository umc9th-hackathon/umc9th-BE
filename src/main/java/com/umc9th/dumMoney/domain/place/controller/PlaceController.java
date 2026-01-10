package com.umc9th.dumMoney.domain.place.controller;

import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    // API: 내 위치 주변 장소 검색
    @GetMapping("/search")
    public ResponseEntity<PlaceSearchResponseDto> searchNearbyPlaces(
            @RequestParam("memberId") Long memberId,
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng
            // 추후 반경(radius)이나 카테고리(category)도 여기서 @RequestParam으로 받을 수 있습니다.
    ) {
        PlaceSearchResponseDto response = placeService.searchPlaces(memberId, lat, lng);
        return ResponseEntity.ok(response);
    }
}
