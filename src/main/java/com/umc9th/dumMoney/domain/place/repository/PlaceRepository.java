package com.umc9th.dumMoney.domain.place.repository;

import com.umc9th.dumMoney.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 내 위치(lat, lng)를 기준으로 반경(radius) 내의 장소를 거리순으로 조회
     * H2/MySQL 호환 하버사인 공식 사용
     * 반환값: Object[] (0:id, 1:name, 2:category, 3:lat, 4:lng, 5:roadAddress, 6:url, 7:openingHours, 8:distance)
     *
     * H2에서도 동작하고 나중에 MySQL에서도 동작하는 Native Query
     * Entity 자체를 반환받으면 distance 값을 담을 곳이 없으므로 필요한 컬럼만 조회(Object[])하는 방식을 사용
     */
    @Query(value = "SELECT " +
            "p.place_id, " +
            "p.name, " +
            "p.category, " +
            "p.lat, " +
            "p.lng, " +
            "p.road_address, " +
            "p.url, " +
            "p.opening_hour, " +
            "(6371000 * acos(cos(radians(:lat)) * cos(radians(p.lat)) * cos(radians(p.lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(p.lat)))) AS distance " +
            "FROM place p " +
            "HAVING distance <= :radius " +
            "ORDER BY distance ASC", nativeQuery = true)
    List<Object[]> findPlacesByDistance(@Param("lat") double lat,
                                        @Param("lng") double lng,
                                        @Param("radius") double radius);
}
