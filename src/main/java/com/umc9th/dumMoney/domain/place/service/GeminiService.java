package com.umc9th.dumMoney.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc9th.dumMoney.domain.place.entity.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public String getRecommendation(List<Place> places, String userPrompt, String routeInfo) {

        /* * [최종 수정]
         * v1beta -> v1 으로 변경합니다. (Dart 코드와 동일)
         * 모델명: gemini-1.5-flash
         */
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey.trim();

        log.info("[Gemini Request URL] {}", url); // URL 확인용 로그

        // 1. 후보 장소 데이터 변환
        StringBuilder placeInfo = new StringBuilder();
        for (Place p : places) {
            placeInfo.append(String.format(
                    "- 가게명: %s | 메뉴: %s | 가격: %d원 | 위치: %f, %f | 특징: %s\n",
                    p.getName(),
                    // Entity 필드명 확인 (getMenuName 없으면 getMenu 등으로 변경)
                    p.getMenuName(),
                    p.getMenuPrice(),
                    p.getLat(),
                    p.getLng(),
                    p.getDescription()
            ));
        }

        // 2. 프롬프트 작성
        String prompt = """
            [Role]
            합리적인 소비 큐레이터.

            [Context]
            이동 경로: "%s"
            사용자 니즈: "%s"

            [Candidates]
            %s

            [Task]
            1. 사용자의 이동 경로와 니즈를 고려해 핵심 키워드 2개 추출
            2. 가성비 좋은 가게 3곳 추천 (순위 포함)
            3. 이동수단 및 비용 계산
            4. totalCost = 메뉴가격 + 이동비용

            [Output Format]
            JSON Only.

            {
              "keywords": ["키워드1", "키워드2"],
              "recommendations": [
                {
                  "rank": 1,
                  "transport": "도보",
                  "shopName": "가게명",
                  "menu": "메뉴명",
                  "itemPrice": 2000,
                  "transportCost": 0,
                  "totalCost": 2000
                }
              ]
            }
            """.formatted(routeInfo, userPrompt, placeInfo);

        // 3. API 요청 (v1은 generationConfig 호환성이 좋음)
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            return extractJsonFromGeminiResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Gemini API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"AI 서버 연결 오류 (" + e.getStatusCode() + ")\"}";
        } catch (Exception e) {
            log.error("Gemini Unknown Error", e);
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"내부 오류 발생\"}";
        }
    }

    private String extractJsonFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidate = root.path("candidates").get(0);

            if (candidate.isMissingNode()) {
                return "{\"keywords\": [], \"recommendations\": [], \"message\": \"AI 응답 없음\"}";
            }

            String text = candidate.path("content").path("parts").get(0).path("text").asText();

            if (text.startsWith("```")) {
                text = text.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "");
            }
            return text.trim();
        } catch (Exception e) {
            log.error("JSON Parsing Error", e);
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"응답 분석 오류\"}";
        }
    }
}