package com.umc9th.dumMoney.domain.member.dto;

import com.umc9th.dumMoney.domain.member.enums.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OnboardingRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotNull(message = "예산은 필수입니다.")
    @Min(value = 1, message = "예산은 1원 이상이어야 합니다.")
    private Integer budget;

    @NotNull(message = "거리는 필수입니다.")
    private Integer distance; // 300, 500, 1000, 1500 (meters)

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    private Double lat; // 위도

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    private Double lng; // 경도

    public OnboardingRequest(Category category, Integer budget, Integer distance, Double lat, Double lng) {
        this.category = category;
        this.budget = budget;
        this.distance = distance;
        this.lat = lat;
        this.lng = lng;
    }
}
