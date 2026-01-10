package com.umc9th.dumMoney.domain.member.controller;

import com.umc9th.dumMoney.domain.member.dto.LocationUpdateRequest;
import com.umc9th.dumMoney.domain.member.dto.LocationUpdateResponse;
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

@Tag(name = "멤버", description = "멤버 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/member/test")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.FORBIDDEN, ErrorCode.NOT_FOUND})
    public String test() {
        return "Hello, World!";
    }

    @Operation(summary = "위치 정보 업데이트", description = "멤버의 위치 정보(위도, 경도)를 업데이트합니다.")
    @PatchMapping("/location")
    @ApiErrorCodeExamples({ErrorCode.BAD_REQUEST, ErrorCode.MEMBER_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ApiResponse<LocationUpdateResponse> updateLocation(
            @RequestHeader(value = "X-Member-Id") Long memberId,
            @Valid @RequestBody LocationUpdateRequest request) {
        LocationUpdateResponse response = memberService.updateLocation(memberId, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
