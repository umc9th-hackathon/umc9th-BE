package com.umc9th.dumMoney.global.apiPayload.handler;



import com.umc9th.dumMoney.global.apiPayload.ApiResponse;
import com.umc9th.dumMoney.global.apiPayload.code.BaseErrorCode;
import com.umc9th.dumMoney.global.apiPayload.code.ErrorCode;
import com.umc9th.dumMoney.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GeneralExceptionAdvice {

    // 애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(GeneralException ex){
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiResponse.onFailure(ex.getCode(), null));
    }



    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex, HttpServletRequest request){

        // 오류난 url과 httpMethod 가져오기
        String url = request.getRequestURI();
        String method = request.getMethod();

        // 로그 출력 (더 자세한 정보)
        log.error("[500 ERROR] {} {} - {}", method, url, ex.getMessage(), ex);
        if (ex.getCause() != null) {
            log.error("[500 ERROR] Caused by: {}", ex.getCause().getMessage(), ex.getCause());
        }

        BaseErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "예기치 않은 서버 에러가 발생했습니다.";
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, errorMessage));
    }

}
