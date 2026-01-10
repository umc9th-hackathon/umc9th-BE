package com.umc9th.dumMoney.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    CAFE("카페"),
    RESTAURANT("식사");

    private final String description;
}
