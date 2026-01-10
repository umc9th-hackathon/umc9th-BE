package com.umc9th.dumMoney.domain.member.service;

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

        // 게스트 사용자용 새로운 Member 생성 (lat, lng는 null로 설정, 추후 위치 정보 업데이트 가능)
        Member member = Member.builder()
                .currentBudget(request.getBudget())
                .searchRadius(request.getDistance())
                .targetCategory(request.getCategory())
                .lat(null) // 위치 정보는 나중에 업데이트
                .lng(null) // 위치 정보는 나중에 업데이트
                .build();

        Member savedMember = memberRepository.save(member);
        return PreferenceResponse.from(savedMember);
    }

    public PreferenceResponse getPreference(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        return PreferenceResponse.from(member);
    }

    private boolean isValidDistance(Integer distance) {
        return distance != null && (distance == 300 || distance == 500 || distance == 1000 || distance == 1500);
    }
}
