package com.emailagent.dto.response.onboarding;

import com.emailagent.domain.entity.RagJob;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OnboardingTemplateJobStatusResponse extends BaseResponse {

    @JsonProperty("all_completed")
    private boolean allCompleted;

    @JsonProperty("has_failure")
    private boolean hasFailure;

    @JsonProperty("completed_count")
    private int completedCount;

    @JsonProperty("processing_count")
    private int processingCount;

    @JsonProperty("failed_count")
    private int failedCount;

    private List<JobItem> jobs;

    public static OnboardingTemplateJobStatusResponse of(List<RagJob> jobs) {
        int completedCount = (int) jobs.stream().filter(job -> job.getStatus() == com.emailagent.domain.enums.RagJobStatus.COMPLETED).count();
        int processingCount = (int) jobs.stream().filter(job -> job.getStatus() == com.emailagent.domain.enums.RagJobStatus.PROCESSING || job.getStatus() == com.emailagent.domain.enums.RagJobStatus.QUEUED).count();
        int failedCount = (int) jobs.stream().filter(job -> job.getStatus() == com.emailagent.domain.enums.RagJobStatus.FAILED).count();

        return OnboardingTemplateJobStatusResponse.builder()
                .allCompleted(!jobs.isEmpty() && completedCount == jobs.size())
                .hasFailure(failedCount > 0)
                .completedCount(completedCount)
                .processingCount(processingCount)
                .failedCount(failedCount)
                .jobs(jobs.stream().map(JobItem::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class JobItem {
        @JsonProperty("job_id")
        private String jobId;

        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("job_type")
        private String jobType;

        private String status;

        @JsonProperty("progress_step")
        private String progressStep;

        @JsonProperty("progress_message")
        private String progressMessage;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;

        @JsonProperty("target_id")
        private String targetId;

        @JsonProperty("completed_at")
        private LocalDateTime completedAt;

        public static JobItem from(RagJob job) {
            return JobItem.builder()
                    .jobId(job.getJobId())
                    .requestId(job.getRequestId())
                    .jobType(job.getJobType())
                    .status(job.getStatus().name())
                    .progressStep(job.getProgressStep())
                    .progressMessage(job.getProgressMessage())
                    .errorCode(job.getErrorCode())
                    .errorMessage(job.getErrorMessage())
                    .targetId(job.getTargetId())
                    .completedAt(job.getCompletedAt())
                    .build();
        }
    }
}
