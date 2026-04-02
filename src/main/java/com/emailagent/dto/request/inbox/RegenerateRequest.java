package com.emailagent.dto.request.inbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegenerateRequest {

    @JsonProperty("previous_draft")
    private String previousDraft;
}