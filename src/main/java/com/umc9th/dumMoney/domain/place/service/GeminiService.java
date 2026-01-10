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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getRecommendation(List<Place> places, String userPrompt, String routeInfo) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API 키가 설정되지 않았습니다!");
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"API 키가 설정되지 않았습니다.\"}";
        }
        
        // Gemini API 엔드포인트 설정 (공식 문서 기준)
        // 모델명: gemini-2.5-flash (최신) 또는 gemini-1.5-flash
        String apiVersion = "v1beta";
        String modelName = "gemini-2.5-flash"; // 문서에서 권장하는 최신 모델
        
        // API 키는 헤더로 전달하는 것이 표준 (문서 기준)
        // URL에는 키 파라미터 제거
        String url = String.format("https://generativelanguage.googleapis.com/%s/models/%s:generateContent", 
                apiVersion, modelName);
        
        log.info("[Gemini API] 버전: {}, 모델: {}, URL: {}", apiVersion, modelName, url);

        // 1. 후보 장소 데이터 변환
        if (places.isEmpty()) {
            log.warn("전달된 후보 장소가 없습니다!");
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"추천할 장소가 없습니다.\"}";
        }
        
        StringBuilder placeInfo = new StringBuilder();
        int index = 1;
        for (Place p : places) {
            placeInfo.append(String.format(
                    "%d. 가게명: %s | 메뉴: %s | 가격: %d원 | 위치: (%f, %f) | 특징: %s\n",
                    index++,
                    p.getName(),
                    p.getMenuName(),
                    p.getMenuPrice(),
                    p.getLat(),
                    p.getLng(),
                    p.getDescription() != null ? p.getDescription() : "특징 없음"
            ));
        }
        
        log.info("Gemini에게 전달할 후보 장소 정보:\n{}", placeInfo.toString());

        // 2. 프롬프트 작성
        // [중요] Candidates에 있는 실제 DB 데이터만 사용하도록 명시
        String prompt = """
            [Role]
            합리적인 소비 큐레이터.

            [Context]
            이동 경로: "%s"
            사용자 니즈: "%s"

            [Candidates - 반드시 여기 나열된 가게만 추천해야 함]
            %s

            [Task]
            1. 사용자의 이동 경로와 니즈를 고려해 핵심 키워드 2개 추출
            2. 위 [Candidates] 섹션에 나열된 가게 중에서만 가성비 좋은 가게를 최대 3곳 추천 (순위 포함)
            3. Candidates에 없는 가게는 절대 추천하지 마세요. 오직 Candidates에 나열된 가게명, 메뉴, 가격만 사용하세요.
            4. 이동수단 및 비용 계산
            5. totalCost = 메뉴가격 + 이동비용

            [Output Format]
            JSON Only. 반드시 Candidates에 있는 실제 가게명, 메뉴명, 가격을 사용하세요.

            {
              "keywords": ["키워드1", "키워드2"],
              "recommendations": [
                {
                  "rank": 1,
                  "transport": "도보",
                  "shopName": "Candidates에 있는 실제 가게명",
                  "menu": "Candidates에 있는 실제 메뉴명",
                  "itemPrice": Candidates에 있는 실제 가격,
                  "transportCost": 0,
                  "totalCost": 실제 가격
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
        // 공식 문서 기준: API 키를 헤더로 전달
        headers.set("x-goog-api-key", apiKey.trim());

        try {
            log.info("[Gemini API] 요청 시작 - 헤더에 API 키 포함됨");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            log.info("[Gemini API] 응답 성공 - Status: {}", response.getStatusCode());
            return extractJsonFromGeminiResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Gemini API Error: {} - Response Body: {}", e.getStatusCode(), errorBody);
            log.error("Request URL: {}", url.replace(apiKey.trim(), "***API_KEY***"));
            
            // 404 오류일 경우 사용 가능한 모델 확인 안내
            if (e.getStatusCode().value() == 404) {
                log.error("모델을 찾을 수 없습니다. 사용 가능한 모델 목록을 확인하려면 다음 URL을 호출하세요:");
                log.error("https://generativelanguage.googleapis.com/v1beta/models?key={API_KEY}");
                return "{\"keywords\": [], \"recommendations\": [], \"message\": \"AI 모델을 찾을 수 없습니다. API 키나 모델명을 확인하거나, Google AI Studio (https://aistudio.google.com/)에서 사용 가능한 모델을 확인해주세요.\"}";
            }
            
            // 403 오류는 API 키 권한 문제
            if (e.getStatusCode().value() == 403) {
                return "{\"keywords\": [], \"recommendations\": [], \"message\": \"API 키 권한이 없습니다. Google AI Studio에서 API 키를 확인해주세요.\"}";
            }
            
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"AI 서버 연결 오류 (" + e.getStatusCode() + ")\"}";
        } catch (Exception e) {
            log.error("Gemini Unknown Error", e);
            return "{\"keywords\": [], \"recommendations\": [], \"message\": \"내부 오류 발생: " + e.getMessage() + "\"}";
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