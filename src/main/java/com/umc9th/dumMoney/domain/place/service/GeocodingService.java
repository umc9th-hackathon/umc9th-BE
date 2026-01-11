package com.umc9th.dumMoney.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 자연어 장소명을 위도/경도로 변환 (하드코딩 + Google Geocoding API 사용)
     * @param location 자연어 장소명 (예: "중앙대 정문", "강남역 2번 출구", "서울 강남구")
     * @return [lat, lng] 배열, 변환 실패 시 null
     */
    public double[] geocode(String location) {
        if (location == null || location.trim().isEmpty()) {
            log.warn("장소명이 비어있습니다.");
            return null;
        }

        String trimmedLocation = location.trim();
        
        // 하드코딩: 강남역과 선릉역 좌표 직접 반환
        if (trimmedLocation.contains("강남역")) {
            double lat = 37.498095;  // 강남역 위도
            double lng = 127.027610; // 강남역 경도
            log.info("[하드코딩] '{}' → ({}, {}) [강남역]", trimmedLocation, lat, lng);
            return new double[]{lat, lng};
        }
        
        if (trimmedLocation.contains("선릉역")) {
            double lat = 37.504498;  // 선릉역 위도
            double lng = 127.049005; // 선릉역 경도
            log.info("[하드코딩] '{}' → ({}, {}) [선릉역]", trimmedLocation, lat, lng);
            return new double[]{lat, lng};
        }

        // 하드코딩된 장소가 아니면 Google Geocoding API 호출
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Google Maps API 키가 설정되지 않았습니다. application.yml에 google.maps.api.key를 설정하세요.");
            return null;
        }

        try {
            // Google Geocoding API URL
            String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", location)
                    .queryParam("key", apiKey.trim())
                    .queryParam("language", "ko")  // 한국어 결과
                    .toUriString();

            log.info("[Geocoding API] 장소명 '{}' 변환 시도", location);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Geocoding API 호출 실패: {}", response.getStatusCode());
                return null;
            }

            // JSON 응답 파싱
            JsonNode root = objectMapper.readTree(response.getBody());
            
            // 상태 확인
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.error("Geocoding API 상태 오류: {} - {}", status, root.path("error_message").asText(""));
                return null;
            }

            // 첫 번째 결과의 좌표 추출
            JsonNode results = root.path("results");
            if (!results.isArray() || results.size() == 0) {
                log.warn("장소명 '{}'에 대한 검색 결과가 없습니다.", location);
                return null;
            }

            JsonNode firstResult = results.get(0);
            JsonNode geometry = firstResult.path("geometry");
            JsonNode locationNode = geometry.path("location");

            double lat = locationNode.path("lat").asDouble();
            double lng = locationNode.path("lng").asDouble();

            String formattedAddress = firstResult.path("formatted_address").asText("");

            log.info("[Geocoding 성공] '{}' → ({}, {}) [{}]", location, lat, lng, formattedAddress);

            return new double[]{lat, lng};

        } catch (Exception e) {
            log.error("Geocoding API 호출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 여러 장소명을 한 번에 변환
     * @param locations 장소명 배열
     * @return Map<장소명, [lat, lng]>
     */
    public Map<String, double[]> geocodeBatch(String... locations) {
        Map<String, double[]> result = new HashMap<>();
        for (String location : locations) {
            if (location != null && !location.trim().isEmpty()) {
                double[] coordinates = geocode(location);
                if (coordinates != null) {
                    result.put(location, coordinates);
                }
            }
        }
        return result;
    }
}

