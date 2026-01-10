package com.umc9th.dumMoney.domain.place.service;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.repository.MemberRepository;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceListResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.entity.Place;
import com.umc9th.dumMoney.domain.place.repository.PlaceRepository;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import com.umc9th.dumMoney.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] 로그용
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final GeminiService geminiService; // [추가] AI 서비스 주입

    // 1. 기존 지도 검색 기능 (수정 없음, 미터 단위 유지)
    public PlaceSearchResponseDto searchPlaces(Long memberId, double lat, double lng) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorCode.MEMBER_NOT_FOUND));

        double radius = member.getSearchRadius().doubleValue(); // 미터(m) 단위

        List<Object[]> results = placeRepository.findPlacesByDistance(lat, lng, radius);

        List<PlaceListResponseDto> placeDtos = results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PlaceSearchResponseDto.builder()
                .count(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    // 장소 상세 조회
    public PlaceDetailResponseDto getPlaceDetail(Long placeId) {
        // 1. DB에서 ID로 장소 조회 (없으면 예외 발생)
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));

        // 2. Entity -> DTO 변환 후 반환
        return PlaceDetailResponseDto.from(place);
    }

    // 2. [신규 추가] AI 맞춤형 추천 기능
    public String recommendPlaces(Long memberId, String userPrompt) {
        // (1) 사용자 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorCode.MEMBER_NOT_FOUND));

        // (2) AI에게 넘길 후보 장소 검색 (Entity List)
        // radius와 budget을 조건으로 검색 (미터 단위 그대로 전달)
        List<Place> candidates = placeRepository.findNearbyPlacesForAI(
                member.getLat(),
                member.getLng(),
                member.getSearchRadius().doubleValue(), // radius (m)
                member.getMinBudget(),               // budget
                member.getMaxBudget()
        );

        log.info("AI 추천 후보군 개수: {}개", candidates.size());

        if (candidates.isEmpty()) {
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"조건에 맞는 주변 가게가 없습니다.\"}";
        }

        // (3) Gemini 호출
        return geminiService.getRecommendation(candidates, userPrompt);
    }

    // DTO 매핑 헬퍼 메서드
    private PlaceListResponseDto mapToDto(Object[] result) {
        return PlaceListResponseDto.builder()
                .id((Long) result[0])
                .name((String) result[1])
                .category(result[2].toString()) // Enum -> String 안전하게 변환
                .latitude((Double) result[3])
                .longitude((Double) result[4])
                .address((String) result[5])
                .imageUrl((String) result[6])
                .openingHours((String) result[7])
                .distance((Double) result[8]) // 거리(m)
                .build();
    }
}