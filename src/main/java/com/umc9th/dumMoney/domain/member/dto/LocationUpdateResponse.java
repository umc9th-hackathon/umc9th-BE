package com.umc9th.dumMoney.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LocationUpdateResponse {
    private Long memberId;
    private Location location;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class Location {
        private Double lat;
        private Double lng;
    }
}
