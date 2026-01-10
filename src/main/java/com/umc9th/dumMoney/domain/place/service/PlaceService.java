package com.umc9th.dumMoney.domain.place.service;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.repository.MemberRepository;
import com.umc9th.dumMoney.domain.place.dto.request.RecommendationRequestDto; // [필수] DTO Import
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceListResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.entity.Place;
import com.umc9th.dumMoney.domain.place.repository.PlaceRepository;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import com.umc9th.dumMoney.global.apiPayload.exception.GeneralException;
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
    public PlaceSearchResponseDto searchPlaces(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + memberId));

        // [추가] 필수 데이터 유효성 검사
        if (member.getLat() == null || member.getLng() == null || member.getSearchRadius() == null) {
            throw new GeneralException(ErrorCode.MEMBER_LOCATION_NOT_SET);
        }

        double radius = member.getSearchRadius().doubleValue();
        double memberLat = member.getLat(); // [변경] DB에 저장된 위도 사용
        double memberLng = member.getLng(); // [변경] DB에 저장된 경도 사용

        List<Object[]> results = placeRepository.findPlacesByDistance(memberLat, memberLng, radius);

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

        // [핵심 로직] 출발지/도착지가 있으면 -> 직선 경로를 300m 간격으로 분할하여 검색
        List<Place> candidates;
        String routeInfo;
        
        if (request.getStartLat() != null && request.getEndLat() != null) {
            // 1. 출발지와 도착지 사이 전체 거리 계산
            double totalDistance = getDistance(
                    request.getStartLat(), request.getStartLng(),
                    request.getEndLat(), request.getEndLng()
            );
            
            // 출발지와 도착지가 같은 경우 처리 (거리 0)
            if (totalDistance < 10.0) { // 10m 이하면 같은 위치로 간주
                log.info("출발지와 도착지가 동일한 위치입니다. 해당 위치 기준 반경 500m 내 검색");
                
                String categoryStr = determineCategoryFromPrompt(request.getUserPrompt());
                candidates = placeRepository.findNearbyPlacesForAI(
                        request.getStartLat(),
                        request.getStartLng(),
                        500.0, // 반경 500m
                        categoryStr,
                        member.getMinBudget(),
                        member.getMaxBudget()
                );
                
                routeInfo = String.format("위치(%f, %f) 주변 500m 내 장소 검색",
                        request.getStartLat(), request.getStartLng());
                
                log.info("검색 조건: 카테고리={}, 예산={}~{}, 반경=500m, 좌표=({}, {})", 
                        categoryStr, member.getMinBudget(), member.getMaxBudget(), 
                        request.getStartLat(), request.getStartLng());
                
            } else {
                // 2. 직선 경로를 300m 간격으로 분할하여 기준점 생성
                double interval = 300.0; // 300m 간격
                List<double[]> waypoints = generateWaypoints(
                        request.getStartLat(), request.getStartLng(),
                        request.getEndLat(), request.getEndLng(),
                        interval
                );
                
                log.info("경로 검색 모드: 전체 거리={}m, 기준점 개수={}개 (300m 간격)", totalDistance, waypoints.size());
                
                // 3. 각 기준점에서 반경 500m 이내 장소 검색 및 수집
                candidates = new java.util.ArrayList<>();
                String categoryStr = determineCategoryFromPrompt(request.getUserPrompt());
                
                for (double[] waypoint : waypoints) {
                    double waypointLat = waypoint[0];
                    double waypointLng = waypoint[1];
                    double searchRadius = 500.0; // 각 기준점에서 500m 반경
                    
                    List<Place> placesAtWaypoint = placeRepository.findNearbyPlacesForAI(
                            waypointLat,
                            waypointLng,
                            searchRadius,
                            categoryStr,
                            member.getMinBudget(),
                            member.getMaxBudget()
                    );
                    
                    // 중복 제거 (place_id 기준)
                    for (Place place : placesAtWaypoint) {
                        boolean isDuplicate = candidates.stream()
                                .anyMatch(p -> p.getId().equals(place.getId()));
                        if (!isDuplicate) {
                            candidates.add(place);
                        }
                    }
                }
                
                // 4. 최종 검증: 각 장소가 실제로 경로에서 500m 이내인지 확인
                candidates = filterPlacesWithinRoute(
                        candidates,
                        request.getStartLat(), request.getStartLng(),
                        request.getEndLat(), request.getEndLng(),
                        500.0
                );
                
                routeInfo = String.format("출발지(%f, %f)에서 도착지(%f, %f)로 이동 중, 경로 상 500m 이내 장소 검색",
                        request.getStartLat(), request.getStartLng(),
                        request.getEndLat(), request.getEndLng());
                
                log.info("최종 후보군 개수: {}개 (경로 상 500m 이내 필터링 후)", candidates.size());
            }
            
        } else {
            // 출발/도착지 없으면 내 위치 기준 (기본)
            double searchLat = member.getLat();
            double searchLng = member.getLng();
            double searchRadius = member.getSearchRadius().doubleValue();
            
            String categoryStr = determineCategoryFromPrompt(request.getUserPrompt());
            log.info("프롬프트 분석 결과: '{}' → 카테고리={}", request.getUserPrompt(), categoryStr);
            
            candidates = placeRepository.findNearbyPlacesForAI(
                    searchLat,
                    searchLng,
                    searchRadius,
                    categoryStr,
                    member.getMinBudget(),
                    member.getMaxBudget()
            );
            
            routeInfo = "현재 위치 주변 탐색 중";
            log.info("검색 조건: 카테고리={}, 예산={}~{}, 반경={}m, 좌표=({}, {})", 
                    categoryStr, member.getMinBudget(), member.getMaxBudget(), searchRadius, searchLat, searchLng);
        }

        log.info("DB에서 찾은 후보군 개수: {}개", candidates.size());

        if (candidates.isEmpty()) {
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"조건에 맞는 주변 가게가 없습니다.\"}";
        }

        // [성능 향상] 경로 이탈 거리와 가격 기반 가중치 점수 계산 및 정렬
        // 출발지/도착지가 있고, 거리가 0이 아닌 경우에만 경로 기반 점수 계산
        if (request.getStartLat() != null && request.getEndLat() != null) {
            double totalDistance = getDistance(
                    request.getStartLat(), request.getStartLng(),
                    request.getEndLat(), request.getEndLng()
            );
            
            if (totalDistance >= 10.0) {
                // 경로가 있는 경우: 경로 이탈 거리와 가격 가중치 계산
                candidates = calculateAndSortByScore(
                        candidates,
                        request.getStartLat(), request.getStartLng(),
                        request.getEndLat(), request.getEndLng(),
                        member.getMinBudget(),
                        member.getMaxBudget()
                );
            } else {
                // 출발지=도착지인 경우: 가격만으로 정렬 (거리는 모두 동일하므로)
                candidates = candidates.stream()
                        .sorted((a, b) -> Integer.compare(a.getMenuPrice(), b.getMenuPrice())) // 가격 낮은 순
                        .collect(Collectors.toList());
                log.info("출발지=도착지: 가격 기준으로 정렬 완료 (가격 낮은 순)");
            }
            
            // 상위 10개만 Gemini에게 전달 (너무 많으면 비용 증가 및 성능 저하)
            int maxCandidates = Math.min(10, candidates.size());
            candidates = candidates.subList(0, maxCandidates);
            
            log.info("가중치 점수 계산 후 상위 {}개 장소 선정 완료", maxCandidates);
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

    /**
     * 출발지에서 도착지까지 직선 경로를 interval 간격으로 분할하여 중간 지점(waypoint) 리스트 생성
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @param interval 간격 (미터)
     * @return 중간 지점들의 [lat, lng] 배열 리스트
     */
    private List<double[]> generateWaypoints(double startLat, double startLng, double endLat, double endLng, double interval) {
        List<double[]> waypoints = new java.util.ArrayList<>();
        
        // 전체 거리 계산
        double totalDistance = getDistance(startLat, startLng, endLat, endLng);
        
        // 출발지도 첫 번째 기준점으로 포함
        waypoints.add(new double[]{startLat, startLng});
        
        // Bearing (방위각) 계산
        double bearing = getBearing(startLat, startLng, endLat, endLng);
        
        // 300m 간격으로 중간 지점 생성
        double currentDistance = interval;
        while (currentDistance < totalDistance) {
            double[] waypoint = getDestinationPoint(startLat, startLng, bearing, currentDistance);
            waypoints.add(waypoint);
            currentDistance += interval;
        }
        
        // 도착지도 마지막 기준점으로 포함
        waypoints.add(new double[]{endLat, endLng});
        
        return waypoints;
    }

    /**
     * 두 점 사이의 방위각(Bearing) 계산 (도 단위, 0~360)
     */
    private double getBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - 
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360; // 0~360 범위로 정규화
    }

    /**
     * 시작점에서 특정 방위각과 거리로 이동한 목적지 좌표 계산
     */
    private double[] getDestinationPoint(double lat, double lon, double bearing, double distance) {
        int R = 6371000; // 지구 반지름 (미터)
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double bearingRad = Math.toRadians(bearing);
        double angularDistance = distance / R;
        
        double destLat = Math.asin(
                Math.sin(latRad) * Math.cos(angularDistance) +
                Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        );
        
        double destLon = lonRad + Math.atan2(
                Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
                Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(destLat)
        );
        
        return new double[]{Math.toDegrees(destLat), Math.toDegrees(destLon)};
    }

    /**
     * 장소 리스트 중에서 실제로 경로(출발지-도착지 직선)에서 maxDistance 이내에 있는 것만 필터링
     * 점과 선분 사이의 최단 거리 계산
     */
    private List<Place> filterPlacesWithinRoute(List<Place> places, 
                                                double startLat, double startLng,
                                                double endLat, double endLng,
                                                double maxDistance) {
        return places.stream()
                .filter(place -> {
                    double distanceToRoute = getDistanceToLineSegment(
                            place.getLat(), place.getLng(),
                            startLat, startLng,
                            endLat, endLng
                    );
                    return distanceToRoute <= maxDistance;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 점과 선분(출발지-도착지) 사이의 최단 거리 계산
     * 선분 위의 가장 가까운 점을 찾고, 그 점과의 거리를 반환
     */
    private double getDistanceToLineSegment(double pointLat, double pointLng,
                                            double lineStartLat, double lineStartLng,
                                            double lineEndLat, double lineEndLng) {
        // 선분의 길이
        double lineLength = getDistance(lineStartLat, lineStartLng, lineEndLat, lineEndLng);
        
        if (lineLength < 0.001) { // 선분이 거의 점인 경우
            return getDistance(pointLat, pointLng, lineStartLat, lineStartLng);
        }
        
        // 점에서 선분의 시작점과 끝점까지의 거리
        double distToStart = getDistance(pointLat, pointLng, lineStartLat, lineStartLng);
        double distToEnd = getDistance(pointLat, pointLng, lineEndLat, lineEndLng);
        
        // 선분의 방향 벡터 (정규화)
        double bearing = getBearing(lineStartLat, lineStartLng, lineEndLat, lineEndLng);
        
        // 점에서 선분의 시작점까지의 방향
        double bearingToPoint = getBearing(lineStartLat, lineStartLng, pointLat, pointLng);
        
        // 점에서 선분으로 내린 수선의 발(垂足) 찾기
        // 간단한 방법: 선분을 여러 구간으로 나눠서 가장 가까운 점 찾기
        double minDistance = Math.min(distToStart, distToEnd);
        int segments = Math.max(10, (int)(lineLength / 10)); // 10m 단위로 세분화
        
        for (int i = 0; i <= segments; i++) {
            double ratio = (double) i / segments;
            double[] segmentPoint = interpolatePoint(lineStartLat, lineStartLng, lineEndLat, lineEndLng, ratio);
            double dist = getDistance(pointLat, pointLng, segmentPoint[0], segmentPoint[1]);
            minDistance = Math.min(minDistance, dist);
        }
        
        return minDistance;
    }

    /**
     * 두 점 사이를 ratio(0.0~1.0) 비율로 보간한 좌표 반환
     */
    private double[] interpolatePoint(double lat1, double lon1, double lat2, double lon2, double ratio) {
        double lat = lat1 + (lat2 - lat1) * ratio;
        double lon = lon1 + (lon2 - lon1) * ratio;
        return new double[]{lat, lon};
    }

    /**
     * 경로 이탈 거리와 가격을 가중치로 계산하여 점수를 매기고 정렬
     * 가중치: 경로 이탈 거리 60%, 가격 40%
     * 
     * @param places 후보 장소 리스트
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @param minBudget 최소 예산
     * @param maxBudget 최대 예산
     * @return 점수 순으로 정렬된 장소 리스트 (높은 점수 순)
     */
    private List<Place> calculateAndSortByScore(List<Place> places,
                                                double startLat, double startLng,
                                                double endLat, double endLng,
                                                Integer minBudget, Integer maxBudget) {
        if (places.isEmpty()) {
            return places;
        }

        // 가중치 설정 (경로 이탈 거리 60%, 가격 40%)
        double distanceWeight = 0.6;
        double priceWeight = 0.4;
        
        // 가격 범위 정규화를 위한 최소/최대값 계산
        int minPrice = places.stream().mapToInt(Place::getMenuPrice).min().orElse(minBudget);
        int maxPrice = places.stream().mapToInt(Place::getMenuPrice).max().orElse(maxBudget);
        int priceRange = Math.max(1, maxPrice - minPrice); // 0으로 나누기 방지

        // 각 장소에 대해 점수 계산
        List<PlaceWithScore> placesWithScore = places.stream()
                .map(place -> {
                    // 1. 경로 이탈 거리 계산
                    double routeDistance = getDistanceToLineSegment(
                            place.getLat(), place.getLng(),
                            startLat, startLng,
                            endLat, endLng
                    );
                    
                    // 2. 경로 이탈 거리 점수 (0~100점, 0m이면 100점, 500m이면 0점)
                    // 최대 500m 기준으로 정규화 (거리가 가까울수록 높은 점수)
                    double distanceScore = Math.max(0, (500.0 - routeDistance) / 500.0 * 100.0);
                    
                    // 3. 가격 점수 (0~100점, 가격이 낮을수록 높은 점수)
                    // 예산 범위 내에서 정규화
                    int price = place.getMenuPrice();
                    double priceScore;
                    if (price <= minBudget) {
                        priceScore = 100.0; // 최소 예산 이하면 만점
                    } else if (price >= maxBudget) {
                        priceScore = 0.0; // 최대 예산 이상이면 0점
                    } else {
                        // 예산 범위 내에서 정규화: (maxBudget - price) / (maxBudget - minBudget) * 100
                        priceScore = (double)(maxBudget - price) / (maxBudget - minBudget) * 100.0;
                    }
                    
                    // 4. 종합 점수 계산 (가중 평균)
                    double totalScore = (distanceScore * distanceWeight) + (priceScore * priceWeight);
                    
                    log.debug("장소: {} | 경로거리: {}m ({}점) | 가격: {}원 ({}점) | 종합점수: {}점",
                            place.getName(), 
                            String.format("%.1f", routeDistance),
                            String.format("%.1f", distanceScore),
                            price,
                            String.format("%.1f", priceScore),
                            String.format("%.2f", totalScore));
                    
                    return new PlaceWithScore(place, totalScore, routeDistance, priceScore);
                })
                .sorted((a, b) -> Double.compare(b.totalScore, a.totalScore)) // 높은 점수 순 정렬
                .collect(Collectors.toList());
        
        // 점수 순으로 정렬된 Place 리스트 반환
        List<Place> sortedPlaces = placesWithScore.stream()
                .map(p -> p.place)
                .collect(Collectors.toList());
        
        // 로그 출력 (상위 5개)
        log.info("=== 가중치 점수 계산 결과 (상위 5개) ===");
        placesWithScore.stream().limit(5).forEach(p -> {
            log.info("장소: {} | 종합점수: {} | 경로거리: {}m | 가격점수: {}",
                    p.place.getName(), 
                    String.format("%.2f", p.totalScore), 
                    String.format("%.1f", p.routeDistance), 
                    String.format("%.1f", p.priceScore));
        });
        
        return sortedPlaces;
    }

    /**
     * 점수와 함께 장소를 저장하는 내부 클래스
     */
    private static class PlaceWithScore {
        Place place;
        double totalScore;
        double routeDistance;
        double priceScore;
        
        PlaceWithScore(Place place, double totalScore, double routeDistance, double priceScore) {
            this.place = place;
            this.totalScore = totalScore;
            this.routeDistance = routeDistance;
            this.priceScore = priceScore;
        }
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