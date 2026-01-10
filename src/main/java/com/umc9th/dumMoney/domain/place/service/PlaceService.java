package com.umc9th.dumMoney.domain.place.service;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.repository.MemberRepository;
import com.umc9th.dumMoney.domain.place.dto.request.RecommendationRequestDto; // [필수] DTO Import
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceListResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.entity.Place;
import com.umc9th.dumMoney.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GeminiService geminiService;

    // 1. 기존 지도 검색 기능
    public PlaceSearchResponseDto searchPlaces(Long memberId, double lat, double lng) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + memberId));

        double radius = member.getSearchRadius().doubleValue();

        List<Object[]> results = placeRepository.findPlacesByDistance(lat, lng, radius);

        List<PlaceListResponseDto> placeDtos = results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PlaceSearchResponseDto.builder()
                .count(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    // 2. 장소 상세 조회
    public PlaceDetailResponseDto getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장소를 찾을 수 없습니다. id=" + placeId));
        return PlaceDetailResponseDto.from(place);
    }

    // 3. [수정됨] AI 맞춤형 추천 기능 (경로 기반)
    public String recommendPlaces(RecommendationRequestDto request) {
        Long memberId = request.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + memberId));

        double searchLat;
        double searchLng;
        double searchRadius;
        String routeInfo;

        // [핵심 로직] 출발지/도착지가 있으면 -> 그 "사이(중간 지점)"를 검색함
        if (request.getStartLat() != null && request.getEndLat() != null) {
            // 1. 중간 지점(Midpoint) 계산
            searchLat = (request.getStartLat() + request.getEndLat()) / 2.0;
            searchLng = (request.getStartLng() + request.getEndLng()) / 2.0;

            // 2. 두 지점 사이 거리 측정
            double distance = getDistance(request.getStartLat(), request.getStartLng(), request.getEndLat(), request.getEndLng());

            // 3. 반경 설정: (거리의 절반) + 500m 여유 범위
            searchRadius = (distance / 2.0) + 500.0;

            // 4. Gemini에게 줄 경로 정보 생성
            routeInfo = String.format("출발지(%f, %f)에서 도착지(%f, %f)로 이동 중",
                    request.getStartLat(), request.getStartLng(),
                    request.getEndLat(), request.getEndLng());

            log.info("경로 검색 모드: 중간지점({}, {}), 반경({}m)", searchLat, searchLng, searchRadius);
        } else {
            // 출발/도착지 없으면 내 위치 기준 (기본)
            searchLat = member.getLat();
            searchLng = member.getLng();
            searchRadius = member.getSearchRadius().doubleValue();

            routeInfo = "현재 위치 주변 탐색 중";
        }

        // 프롬프트를 분석해서 카테고리 결정 (음료/카페 관련 → CAFE, 식사/음식 관련 → RESTAURANT)
        String categoryStr = determineCategoryFromPrompt(request.getUserPrompt());
        
        log.info("프롬프트 분석 결과: '{}' → 카테고리={}", request.getUserPrompt(), categoryStr);
        
        // DB에서 후보 장소 검색 (출발지/도착지 기반으로 계산한 좌표 사용)
        List<Place> candidates = placeRepository.findNearbyPlacesForAI(
                searchLat,      // 계산된 중간 지점 위도
                searchLng,      // 계산된 중간 지점 경도
                searchRadius,   // 계산된 반경
                categoryStr,    // 프롬프트에서 분석한 카테고리 (CAFE or RESTAURANT)
                member.getMinBudget(),
                member.getMaxBudget()
        );
        
        log.info("검색 조건: 카테고리={}, 예산={}~{}, 반경={}m, 좌표=({}, {})", 
                categoryStr, member.getMinBudget(), member.getMaxBudget(), searchRadius, searchLat, searchLng);

        log.info("DB에서 찾은 후보군 개수: {}개", candidates.size());

        if (candidates.isEmpty()) {
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"조건에 맞는 주변 가게가 없습니다.\"}";
        }

        // Gemini 호출 (candidates, userPrompt, routeInfo 전달)
        log.info("Gemini API 호출 시작 - 후보 개수: {}개, 프롬프트: {}", candidates.size(), request.getUserPrompt());
        return geminiService.getRecommendation(candidates, request.getUserPrompt(), routeInfo);
    }

    // [유틸] 프롬프트를 분석해서 카테고리 결정 (자연어 이해)
    private String determineCategoryFromPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "RESTAURANT"; // 기본값
        }
        
        String prompt = userPrompt.toLowerCase();
        
        // 카페 관련 키워드 (음료, 커피, 카페 등)
        if (prompt.contains("음료") || prompt.contains("커피") || prompt.contains("카페") || 
            prompt.contains("라떼") || prompt.contains("아메리카노") || prompt.contains("음료수") ||
            prompt.contains("디저트") || prompt.contains("케이크") || prompt.contains("빵") ||
            prompt.contains("차") || prompt.contains("티") || prompt.contains("마시") ||
            prompt.contains("마셔") || prompt.contains("마실") || prompt.contains("마셔야") ||
            prompt.contains("마시고") || prompt.contains("마시려") || prompt.contains("마시고 싶")) {
            return "CAFE";
        }
        
        // 식당 관련 키워드 (밥, 식사, 음식 등)
        if (prompt.contains("밥") || prompt.contains("식사") || prompt.contains("음식") || 
            prompt.contains("식당") || prompt.contains("맛집") || prompt.contains("먹") ||
            prompt.contains("한식") || prompt.contains("중식") || prompt.contains("일식") || 
            prompt.contains("양식") || prompt.contains("치킨") || prompt.contains("피자") ||
            prompt.contains("면") || prompt.contains("국수") || prompt.contains("떡볶이") ||
            prompt.contains("먹고") || prompt.contains("먹어") || prompt.contains("먹을") ||
            prompt.contains("먹어야") || prompt.contains("먹고 싶")) {
            return "RESTAURANT";
        }
        
        // 기본값: 키워드가 없으면 식당으로
        return "RESTAURANT";
    }

    // [유틸] 두 좌표 사이 거리 계산 (Haversine 공식, 미터 단위 반환)
    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371000; // 지구 반지름 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // DTO 매핑 헬퍼 메서드
    private PlaceListResponseDto mapToDto(Object[] result) {
        return PlaceListResponseDto.builder()
                .id((Long) result[0])
                .name((String) result[1])
                .category(result[2].toString())
                .latitude((Double) result[3])
                .longitude((Double) result[4])
                .address((String) result[5])
                .imageUrl((String) result[6])
                .openingHours((String) result[7])
                .distance((Double) result[8])
                .build();
    }
}