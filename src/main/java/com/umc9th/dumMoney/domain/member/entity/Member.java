package com.umc9th.dumMoney.domain.member.entity;

import com.umc9th.dumMoney.domain.member.enums.Category;
import com.umc9th.dumMoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "current_budget", nullable = false)
    private Integer currentBudget;

    @Column(name = "search_radius", nullable = false)
    private Integer searchRadius;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_category", nullable = false, length = 20)
    private Category targetCategory;

    @Column(name = "lat", nullable = false)
    private Double lat; // 위도

    @Column(name = "lng", nullable = false)
    private Double lng; // 경도

    @Builder
    public Member(Integer currentBudget, Integer searchRadius, Category targetCategory, Double lat, Double lng) {
        this.currentBudget = currentBudget;
        this.searchRadius = searchRadius;
        this.targetCategory = targetCategory;
        this.lat = lat;
        this.lng = lng;
    }

    public void updateOnboarding(Integer currentBudget, Integer searchRadius, Category targetCategory) {
        this.currentBudget = currentBudget;
        this.searchRadius = searchRadius;
        this.targetCategory = targetCategory;
    }
}
