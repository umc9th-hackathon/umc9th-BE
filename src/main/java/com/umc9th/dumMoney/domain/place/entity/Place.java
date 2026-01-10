package com.umc9th.dumMoney.domain.place.entity;

import com.umc9th.dumMoney.global.common.BaseEntity; // 만약 BaseTimeEntity를 만드셨다면 상속
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 접근 제어 (안전성 UP)
@Table(name = "place") // DB 테이블명 명시
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    // 네이버 지도 ID (중복 방지, Unique)
    @Column(name = "naver_place_id", nullable = false, unique = true)
    private Long naverPlaceId;

    @Column(nullable = false, length = 30)
    private String name;

    // ENUM 타입 매핑 (DB에는 문자열 "CAFE", "RESTAURANT"로 저장됨)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceCategory category;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "road_address", length = 50)
    private String roadAddress;

    @Column(nullable = false, length = 50)
    private String address; // 지번 주소

    @Column(name = "opening_hour", nullable = false, length = 50)
    private String openingHours;

    @Column(length = 255)
    private String description;

    @Column(name = "menu_price", nullable = false)
    private Integer menuPrice;

    @Column(name = "url", length = 255)
    private String imageUrl;

    // 좌표 정보
    @Column(nullable = false)
    private Double lat; // 위도

    @Column(nullable = false)
    private Double lng; // 경도

    // 삭제 시간 (Soft Delete용)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // @Builder 패턴: 생성자 대신 안전하게 객체를 생성
    @Builder
    public Place(Long naverPlaceId, String name, PlaceCategory category, String phone,
                 String roadAddress, String address, String openingHours, String description,
                 Integer menuPrice, String imageUrl, Double lat, Double lng) {
        this.naverPlaceId = naverPlaceId;
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.roadAddress = roadAddress;
        this.address = address;
        this.openingHours = openingHours;
        this.description = description;
        this.menuPrice = menuPrice;
        this.imageUrl = imageUrl;
        this.lat = lat;
        this.lng = lng;
    }

    // 비즈니스 로직: 장소 정보 수정 기능 (Setter 대신 명확한 메서드 사용)
    public void updateInfo(String phone, String openingHours, Integer menuPrice, String description) {
        this.phone = phone;
        this.openingHours = openingHours;
        this.menuPrice = menuPrice;
        this.description = description;
    }
}