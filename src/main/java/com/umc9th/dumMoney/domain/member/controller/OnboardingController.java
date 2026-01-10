package com.umc9th.dumMoney.domain.member.controller;

import com.umc9th.dumMoney.domain.member.dto.OnboardingRequest;
import com.umc9th.dumMoney.domain.member.dto.PreferenceResponse;
import com.umc9th.dumMoney.domain.member.service.MemberService;
import com.umc9th.dumMoney.global.apiPayload.ApiResponse;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import com.umc9th.dumMoney.global.apiPayload.code.SuccessCode;
import com.umc9th.dumMoney.global.config.swagger.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "온보딩", description = "온보딩 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OnboardingController {

    private final MemberService memberService;

    @Operation(summary = "온보딩 설정 저장", description = "게스트 사용자의 온보딩 설정(카테고리, 예산, 거리)을 저장하고 새로운 memberId를 할당합니다.")
    @PostMapping("/onboarding")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> saveOnboarding(
            @Valid @RequestBody OnboardingRequest request) {
        PreferenceResponse response = memberService.createOnboarding(request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(summary = "저장된 온보딩 설정 조회", description = "게스트 사용자의 온보딩 설정을 조회합니다. 온보딩 시 받은 memberId를 헤더로 전달해야 합니다.")
    @GetMapping("/preferences/me")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> getPreference(
            @RequestHeader(value = "X-Member-Id") Long memberId) {
        PreferenceResponse response = memberService.getPreference(memberId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
