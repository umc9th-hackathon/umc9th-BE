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

    @Operation(summary = "온보딩 설정 저장 및 업데이트", description = "사용자의 온보딩 설정(카테고리, 예산, 거리)을 저장하거나 업데이트합니다")
    @PostMapping("/onboarding")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> saveOnboarding(
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId,
            @Valid @RequestBody OnboardingRequest request) {
        // 임시로 memberId가 없으면 1L 사용 (추후 인증 추가 시 변경)
        Long finalMemberId = memberId != null ? memberId : 1L;
        PreferenceResponse response = memberService.saveOrUpdateOnboarding(finalMemberId, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(summary = "저장된 온보딩 설정 조회", description = "현재 사용자의 온보딩 설정을 조회합니다.")
    @GetMapping("/preferences/me")
    @ApiErrorCodeExamples({ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> getPreference(
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId) {
        // 임시로 memberId가 없으면 1L 사용 (추후 인증 추가 시 변경)
        Long finalMemberId = memberId != null ? memberId : 1L;
        PreferenceResponse response = memberService.getPreference(finalMemberId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
