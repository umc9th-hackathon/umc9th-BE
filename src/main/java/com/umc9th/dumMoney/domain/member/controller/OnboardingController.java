package com.umc9th.dumMoney.domain.member.controller;

import com.umc9th.dumMoney.domain.member.dto.MemberUpdateDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "온보딩", description = "온보딩 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OnboardingController {

    private final MemberService memberService;

    @Operation(summary = "온보딩 설정 저장", description = "게스트 사용자의 온보딩 설정(카테고리, 예산, 거리)을 저장하고 새로운 memberId를 할당합니다.")
    @PostMapping("/members")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> saveOnboarding(
            @Valid @RequestBody OnboardingRequest request) {
        PreferenceResponse response = memberService.createOnboarding(request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(summary = "저장된 온보딩 설정 조회", description = "게스트 사용자의 온보딩 설정을 조회합니다. 온보딩 시 받은 memberId를 경로 변수로 전달해야 합니다.")
    @GetMapping("/members/{memberId}")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<PreferenceResponse> getPreference(
            @PathVariable("memberId") Long memberId) {
        PreferenceResponse response = memberService.getPreference(memberId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(summary = "유저의 설정(예산범위, 주변 반경, 카테고리)을 수정",
            description = "유저의 설정(예산범위, 주변 반경, 카테고리)을 수정합니다.")
    @ApiErrorCodeExamples({ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<Void> updateMemberSettings(
            @PathVariable Long memberId,
            @RequestBody MemberUpdateDto request) { // [변경] 모든 정보가 이 안에 있음

        memberService.updateMemberSettings(memberId, request);

        return ResponseEntity.ok().build();
    }
}
