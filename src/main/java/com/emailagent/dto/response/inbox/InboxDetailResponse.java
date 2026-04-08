package com.emailagent.dto.response.inbox;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;



@Getter
@Builder
public class InboxDetailResponse extends BaseResponse {

    @JsonProperty("email_info")
    private EmailInfo emailInfo;

    @JsonProperty("ai_analysis")
    private AiAnalysis aiAnalysis;

    @JsonProperty("draft_reply")
    private DraftReplyInfo draftReply;

    // ── 이메일 원문 ──────────────────────────────
    @Getter
    @Builder
    public static class EmailInfo {
        @JsonProperty("email_id")
        private Long emailId;

        @JsonProperty("sender_name")
        private String senderName;

        @JsonProperty("sender_email")
        private String senderEmail;

        private String subject;
        private String body;

        @JsonProperty("received_at")
        private LocalDateTime receivedAt;

        @JsonProperty("has_attachments")
        private boolean hasAttachments;

        /** 첨부파일 목록 — gmail_attachment_id 미포함, 프론트 전달 안전 */
        private List<AttachmentResponseDto> attachments;
    }

    // ── AI 분석 결과 ─────────────────────────────
    @Getter
    @Builder
    public static class AiAnalysis {
        private String domain;
        private String intent;
        private String summary;
        private Map<String, Object> entities;

        @JsonProperty("confidence_score")
        private BigDecimal confidenceScore;

        @JsonProperty("schedule_detected")
        private Boolean scheduleDetected;

        private Schedule schedule;
    }

    // ── 일정 정보 ────────────────────────────────
    @Getter
    @Builder
    public static class Schedule {
        @JsonProperty("has_schedule")
        private boolean hasSchedule;

        private String title;
        private LocalDate date;

        @JsonProperty("start_time")
        private LocalTime startTime;

        @JsonProperty("end_time")
        private LocalTime endTime;

        private String location;
        private List<String> participants;
    }

    // ── 초안 답장 ────────────────────────────────
    @Getter
    @Builder
    public static class DraftReplyInfo {
        @JsonProperty("draft_id")
        private Long draftId;

        private String status;

        @JsonProperty("template_info")
        private TemplateInfo templateInfo;

        private VariableInfo variables;
        private String subject;
        private String body;
    }

    @Getter
    @Builder
    public static class TemplateInfo {
        @JsonProperty("template_id")
        private Long templateId;

        @JsonProperty("template_title")
        private String templateTitle;
    }

    @Getter
    @Builder
    public static class VariableInfo {
        @JsonProperty("auto_completed_count")
        private int autoCompletedCount;

        @JsonProperty("auto_completed_keys")
        private List<String> autoCompletedKeys;

        @JsonProperty("required_input_count")
        private int requiredInputCount;

        @JsonProperty("required_input_keys")
        private List<String> requiredInputKeys;
    }
}
