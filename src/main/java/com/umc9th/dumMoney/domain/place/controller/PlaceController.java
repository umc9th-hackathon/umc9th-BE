package com.umc9th.dumMoney.domain.place.controller;

import com.umc9th.dumMoney.domain.place.dto.request.RecommendationRequestDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.service.PlaceService;
import com.umc9th.dumMoney.global.apiPayload.ApiResponse;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import com.umc9th.dumMoney.global.apiPayload.code.SuccessCode;
import com.umc9th.dumMoney.global.config.swagger.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "내 위치 기준 주변 가게 목록 API",
            description = "멤버의 위치 정보(위도, 경도)를 받아서 조건에 맞는 가게들의 정보를 반환함.")
    @ApiErrorCodeExamples({ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    @GetMapping("/search")
    public ApiResponse<PlaceSearchResponseDto> searchNearbyPlaces(
            @RequestParam("memberId") Long memberId,
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng
    ) {
        PlaceSearchResponseDto response = placeService.searchPlaces(memberId, lat, lng);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    // API: 특정 장소 상세 조회
    @GetMapping("/{placeId}")
    @ApiErrorCodeExamples({ErrorCode.NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    @Operation(summary = "특정 장소 상세 조회 API",
            description = "핀포인트를 누르면, 해당 가게의 상세 정보를 반환함.")
    public ApiResponse<PlaceDetailResponseDto> getPlaceDetail(@PathVariable Long placeId) {
        PlaceDetailResponseDto response = placeService.getPlaceDetail(placeId);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    // API: AI 맞춤형 소비/경로 추천 (Gemini)
    @PostMapping("/recommend")
    public ResponseEntity<String> recommendPlaces(@RequestBody RecommendationRequestDto request) {
        // 로그에 출발/도착 좌표 정보도 같이 찍히도록 수정함
        log.info("AI 추천 요청 - MemberId: {}, Prompt: {}, Start:({}, {}), End:({}, {})",
                request.getMemberId(),
                request.getUserPrompt(),
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        // [수정 핵심] 개별 파라미터가 아니라 'request' 객체를 통째로 넘겨야 함!
        String jsonResponse = placeService.recommendPlaces(request);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResponse);
    }
}