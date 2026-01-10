package com.umc9th.dumMoney.domain.member.service;

import com.umc9th.dumMoney.domain.member.dto.LocationUpdateRequest;
import com.umc9th.dumMoney.domain.member.dto.LocationUpdateResponse;
import com.umc9th.dumMoney.domain.member.dto.OnboardingRequest;
import com.umc9th.dumMoney.domain.member.dto.PreferenceResponse;
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
            throw new MemberException(ErrorCode.BAD_REQUEST);
        }

        // 게스트 사용자용 새로운 Member 생성 (온보딩 시 위치 정보 포함)
        Member member = Member.builder()
                .currentBudget(request.getBudget())
                .searchRadius(request.getDistance())
                .targetCategory(request.getCategory())
                .lat(request.getLat())
                .lng(request.getLng())
                .build();

        Member savedMember = memberRepository.save(member);
        return PreferenceResponse.from(savedMember);
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

    private boolean isValidDistance(Integer distance) {
        return distance != null && (distance == 300 || distance == 500 || distance == 1000 || distance == 1500);
    }
}
