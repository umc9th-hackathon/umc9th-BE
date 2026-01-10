package com.umc9th.dumMoney.domain.place.controller;
import com.umc9th.dumMoney.domain.place.dto.request.RecommendationRequestDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
@Slf4j
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


    // API: 특정 장소 상세 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceDetailResponseDto> getPlaceDetail(@PathVariable Long placeId) {
        PlaceDetailResponseDto response = placeService.getPlaceDetail(placeId);
        return ResponseEntity.ok(response);
    }
    // API: AI 맞춤형 소비/경로 추천 (Gemini)

    @PostMapping("/recommend")
    public ResponseEntity<String> recommendPlaces(@RequestBody RecommendationRequestDto request) {
        log.info("AI 추천 요청 - MemberId: {}, Prompt: {}", request.getMemberId(), request.getUserPrompt());

        // 1. Service 호출 (DB조회 -> Gemini분석 -> JSON String 반환)
        String jsonResponse = placeService.recommendPlaces(
                request.getMemberId(),
                request.getUserPrompt()
        );

        // 2. 결과 반환
        // JSON 문자열을 객체로 인식시키기 위해 contentType(APPLICATION_JSON) 설정 필수
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResponse);

    }
}
