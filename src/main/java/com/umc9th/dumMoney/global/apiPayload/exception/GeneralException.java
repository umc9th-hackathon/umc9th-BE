package com.umc9th.dumMoney.global.apiPayload.exception;



import com.umc9th.dumMoney.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException{
    private final BaseErrorCode code;

    public GeneralException(BaseErrorCode code) {
        super(code.getMessage()); // [핵심] 부모(RuntimeException)에게 에러 메시지를 넘겨줌
        this.code = code;
    }
}
