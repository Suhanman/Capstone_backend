package com.emailagent.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteIntegrationRequest {

    /**
     * 해제할 대상 서비스: "ALL" (전체 해제) 또는 "CALENDAR" (캘린더 단독 해제)
     */
    @NotBlank
    @Pattern(regexp = "^(ALL|CALENDAR)$", message = "target_service는 ALL 또는 CALENDAR 이어야 합니다.")
    @JsonProperty("target_service")
    private String targetService;
}
