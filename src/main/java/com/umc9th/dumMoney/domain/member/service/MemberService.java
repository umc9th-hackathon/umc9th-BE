package com.umc9th.dumMoney.domain.member.service;

import com.umc9th.dumMoney.domain.member.dto.*;
import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.exception.MemberException;
import com.umc9th.dumMoney.domain.member.repository.MemberRepository;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public PreferenceResponse createOnboarding(OnboardingRequest request) {
        // 거리 검증 (300, 500, 1000, 1500만 허용)
        if (!isValidDistance(request.getDistance())) {
            throw new MemberException(ErrorCode.INVALID_DISTANCE);
        }

        // 예산 범위 검증 (minBudget <= maxBudget)
        if (request.getMinBudget() > request.getMaxBudget()) {
            throw new MemberException(ErrorCode.INVALID_BUDGET_RANGE);
        }

        // 좌표 검증 (null 체크)
        validateLocation(request.getLat(), request.getLng());


        Member member;

        // 2. 분기 처리
        if (request.getMemberId() != null) {
            // [CASE A] 기존 회원 수정
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));

            // Dirty Checking으로 업데이트
            member.updateOnboarding(
                    request.getMinBudget(),
                    request.getMaxBudget(),
                    request.getDistance(),
                    request.getCategory()
            );
            member.updateLocation(request.getLat(), request.getLng());
        } else {
            // [CASE B] 신규 회원 생성 (게스트)
            member = Member.builder()
                    .minBudget(request.getMinBudget())
                    .maxBudget(request.getMaxBudget())
                    .searchRadius(request.getDistance())
                    .targetCategory(request.getCategory())
                    .lat(request.getLat())
                    .lng(request.getLng())
                    .build();

            // 신규 생성 시에는 반드시 save 호출 필요
            memberRepository.save(member);
        }

        // 3. 결과 반환 (여기 담긴 memberId를 프론트가 로컬 스토리지에 저장)
        return PreferenceResponse.from(member);
    }

    public PreferenceResponse getPreference(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        return PreferenceResponse.from(member);
    }

    @Transactional
    public LocationUpdateResponse updateLocation(Long memberId, LocationUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));

        // [추가] 좌표 검증 (null 체크)
        validateLocation(request.getLat(), request.getLng());

        member.updateLocation(request.getLat(), request.getLng());
        Member savedMember = memberRepository.save(member);



        return LocationUpdateResponse.builder()
                .memberId(savedMember.getMemberId())
                .location(LocationUpdateResponse.Location.builder()
                        .lat(savedMember.getLat())
                        .lng(savedMember.getLng())
                        .build())
                .updatedAt(savedMember.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updateMemberSettings(Long memberId, MemberUpdateDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + memberId));

        // 이미 존재하는 메서드를 재사용하여 값을 변경
        member.updateOnboarding(
                request.getMinBudget(),
                request.getMaxBudget(),
                request.getRadius(),
                request.getCategory()
        );
    }

    private boolean isValidDistance(Integer distance) {
        return distance != null && (distance == 300 || distance == 500 || distance == 1000 || distance == 1500);
    }

    // 3. 좌표 검증 로직
    private void validateLocation(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new MemberException(ErrorCode.BAD_REQUEST); // 또는 LOCATION_NULL
        }
    }
}
