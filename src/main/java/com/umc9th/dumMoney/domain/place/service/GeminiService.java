package com.umc9th.dumMoney.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc9th.dumMoney.domain.place.entity.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    public String getRecommendation(List<Place> places, String userPrompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        // 1. 후보 장소 데이터 변환
        StringBuilder placeInfo = new StringBuilder();
        for (Place p : places) {
            placeInfo.append(String.format(
                    "- 가게명: %s | 대표메뉴: %s | 가격: %d원 | 위치: %f, %f | 특징: %s\n",
                    p.getName(), p.getMenuName(), p.getMenuPrice(), p.getLat(), p.getLng(), p.getDescription()
            ));
        }

        // 2. 프롬프트 작성
        String prompt = """
            [Role]
            합리적인 소비 큐레이터.
            
            [Context]
            사용자 니즈: "%s"
            후보 가게 목록:
            %s
            
            [Task]
            1. 사용자 니즈 기반 핵심 '고려사항(keywords)' 2개 추출.
            2. 최적의 가게 3곳 선정 및 1~3위 순위 지정.
            3. 이동수단 제안 및 비용 계산 (도보 0원, 버스 1200원, 택시 4800원).
            4. 총 비용(totalCost) = 메뉴가격 + 이동비용.
            
            [Output Format]
            JSON 형식만 반환 (Markdown 제외, reason 필드 제외).
            
            {
              "keywords": ["키워드1", "키워드2"],
              "recommendations": [
                {
                  "rank": 1,
                  "transport": "도보",
                  "shopName": "가게명",
                  "menu": "대표메뉴명",
                  "itemPrice": 2000,
                  "transportCost": 0,
                  "totalCost": 2000
                }
              ]
            }
            """.formatted(userPrompt, placeInfo.toString());

        // 3. API 요청 구성 및 전송
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);
            return extractJsonFromGeminiResponse(response.getBody());
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            return "{\"keywords\": [], \"recommendations\": []}";
        }
    }

    private String extractJsonFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            if (text.startsWith("```")) {
                text = text.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "");
            }
            return text.trim();
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            throw new RuntimeException("AI 응답 분석 불가");
        }
    }
}