package com.umc9th.dumMoney.domain.place.dto.response;

import com.umc9th.dumMoney.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailResponseDto {
    private Long id;
    // private Long naverPlaceId;
    private String name;
    private String category;
    private String phoneNumber;
    private String roadAddress;
    private String jibunAddress;
    private String openingHours;
    private String description;

    // [New] 대표 메뉴 정보 추가
    private String menuName;
    private Integer menuPrice;

    private String imageUrl;
    private Double latitude;
    private Double longitude;

    // Entity -> DTO 변환
    public static PlaceDetailResponseDto from(Place place) {
        return PlaceDetailResponseDto.builder()
                .id(place.getId())
                // .naverPlaceId(place.getNaverPlaceId())
                .name(place.getName())
                .category(place.getCategory().toString())
                .phoneNumber(place.getPhone())
                .roadAddress(place.getRoadAddress())
                .jibunAddress(place.getAddress())
                .openingHours(place.getOpeningHours())
                .description(place.getDescription())

                // [New] 여기에 엔티티의 값을 넣어줍니다.
                .menuName(place.getMenuName())
                .menuPrice(place.getMenuPrice())

                .imageUrl(place.getImageUrl())
                .latitude(place.getLat())
                .longitude(place.getLng())
                .build();
    }
}