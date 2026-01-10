package com.umc9th.dumMoney.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode {
    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405_1", "허용되지 않은 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "예기치 않은 서버 에러가 발생했습니다."),
    INVALID_DISTANCE(HttpStatus.BAD_REQUEST, "COMMON400_2", "유효하지 않은 거리 값입니다."),
    INVALID_BUDGET_RANGE(HttpStatus.BAD_REQUEST, "COMMON400_3", "최소 예산이 최대 예산보다 클 수 없습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_1", "사용자를 찾을 수 없습니다."),
    MEMBER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "MEMBER409_1", "이미 가입된 사용자입니다."),
    MEMBER_NAME_BAD_REQUEST(HttpStatus.BAD_REQUEST, "MEMBER400_1", "이름의 형식이 맞지 않습니다."),
    MEMBER_LOCATION_NOT_SET(HttpStatus.NOT_FOUND, "COMMON404_2", "위치 설정이 필요합니다"),

    PAGE_INVALID(HttpStatus.BAD_REQUEST, "PAGE400_1", "유효하지 않은 페이지 범위입니다.")
    ;


    private final HttpStatus status;
    private final String code;
    private final String message;



}
