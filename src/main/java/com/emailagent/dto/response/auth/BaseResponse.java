package com.emailagent.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 모든 API 응답의 공통 기반 클래스.
 * 성공 응답: 기본 생성자 사용 (success=true, result_code=200, result_req="")
 * 에러 응답: BaseResponse(int, String) 생성자 사용 (success=false)
 */
@Getter
public class BaseResponse {

    @JsonProperty("content_type")
    private final String contentType = "application/json";

    private boolean success;

    @JsonProperty("result_code")
    private int resultCode;

    @JsonProperty("result_req")
    private String resultReq;

    /** 성공 응답 기본 생성자 */
    public BaseResponse() {
        this.success = true;
        this.resultCode = 200;
        this.resultReq = "";
    }

    /** 에러 응답 생성자 */
    public BaseResponse(int resultCode, String resultReq) {
        this.success = false;
        this.resultCode = resultCode;
        this.resultReq = resultReq;
    }
}
