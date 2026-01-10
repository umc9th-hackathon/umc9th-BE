package com.umc9th.dumMoney.domain.place.service;

import com.umc9th.dumMoney.domain.member.entity.Member;
import com.umc9th.dumMoney.domain.member.repository.MemberRepository;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceDetailResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceListResponseDto;
import com.umc9th.dumMoney.domain.place.dto.response.PlaceSearchResponseDto;
import com.umc9th.dumMoney.domain.place.entity.Place;
import com.umc9th.dumMoney.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository; // 멤버 정보를 가져오기 위해 필요

    public PlaceSearchResponseDto searchPlaces(Long memberId, double lat, double lng) {

        // 1. 멤버 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + memberId));

        // 2. 멤버의 설정 반경 가져오기
        double radius = member.getSearchRadius().doubleValue();

        // 3. Repository 호출
        List<Object[]> results = placeRepository.findPlacesByDistance(lat, lng, radius);

        // 4. Object[] -> DTO 변환
        List<PlaceListResponseDto> placeDtos = results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // 5. 최종 응답 객체 생성
        return PlaceSearchResponseDto.builder()
                .count(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    // 장소 상세 조회
    public PlaceDetailResponseDto getPlaceDetail(Long placeId) {
        // 1. DB에서 ID로 장소 조회 (없으면 예외 발생)
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장소를 찾을 수 없습니다. id=" + placeId));

        // 2. Entity -> DTO 변환 후 반환
        return PlaceDetailResponseDto.from(place);
    }

    private PlaceListResponseDto mapToDto(Object[] result) {
        return PlaceListResponseDto.builder()
                .id((Long) result[0])
                .name((String) result[1])
                .category((String) result[2])
                .latitude((Double) result[3])
                .longitude((Double) result[4])
                .address((String) result[5])
                .imageUrl((String) result[6])
                .openingHours((String) result[7])
                .distance((Double) result[8])
                .build();
    }
}
