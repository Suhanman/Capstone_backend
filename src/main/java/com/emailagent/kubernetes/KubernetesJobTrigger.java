package com.emailagent.kubernetes;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kubernetes Job 트리거 컴포넌트.
 * 기존 TrainingJobPublisher(RabbitMQ 발행)를 대체하며,
 * 무거운 데이터 가공 / S3 업로드 / AI 학습 트리거를 일회용 Go 컨테이너 k8s Job에 위임한다.
 *
 * [Go Job에 전달하는 환경변수]
 * - JOB_ID          : training_jobs 테이블의 PK — Go Job이 작업 식별 및 완료 후 참조용으로 사용
 * - JOB_TYPE        : preprocessing / pair / training / evaluation
 * - TASK_TYPE       : job_type과 동일 (AI 서버 호환용)
 * - DATASET_VERSION : 참조할 데이터셋 버전
 * - REQUESTED_BY    : 요청한 관리자 userId
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KubernetesJobTrigger {

    private final KubernetesClient kubernetesClient;

    @Value("${app.kubernetes.namespace}")
    private String namespace;

    @Value("${app.kubernetes.job-image}")
    private String jobImage;

    @Value("${app.kubernetes.ttl-seconds-after-finished:300}")
    private int ttlSecondsAfterFinished;

    /**
     * k8s Job 생성 — Go 언어 일회용 컨테이너를 파드로 실행.
     *
     * @param jobId          training_jobs PK (UUID)
     * @param jobType        작업 종류 (training / preprocessing / pair / evaluation)
     * @param taskType       실제 수행 작업 (job_type과 동일)
     * @param datasetVersion 참조 데이터셋 버전 (nullable)
     * @param requestedBy    요청자 userId
     */
    public void trigger(String jobId, String jobType, String taskType,
                        String datasetVersion, String requestedBy) {

        // k8s 리소스명: "training-{uuid}" — 최대 45자, 소문자 + 하이픈으로 DNS 규칙 준수
        String k8sJobName = "training-" + jobId;

        Job k8sJob = new JobBuilder()
                .withNewMetadata()
                    .withName(k8sJobName)
                    .withNamespace(namespace)
                    .addToLabels("app", "training-worker")
                    .addToLabels("job-id", jobId)
                    .addToLabels("job-type", jobType)
                .endMetadata()
                .withNewSpec()
                    // Job 완료 후 ttl 초 뒤 자동 삭제
                    .withTtlSecondsAfterFinished(ttlSecondsAfterFinished)
                    // 재시도 없음 — Go Job 내부에서 자체 처리, 결과는 q.2app.training으로 반환
                    .withBackoffLimit(0)
                    .withNewTemplate()
                        .withNewSpec()
                            .withRestartPolicy("Never")
                            .addNewContainer()
                                .withName("training-worker")
                                .withImage(jobImage)
                                .addNewEnv()
                                    .withName("JOB_ID").withValue(jobId)
                                .endEnv()
                                .addNewEnv()
                                    .withName("JOB_TYPE").withValue(jobType)
                                .endEnv()
                                .addNewEnv()
                                    .withName("TASK_TYPE").withValue(taskType)
                                .endEnv()
                                .addNewEnv()
                                    .withName("DATASET_VERSION")
                                    .withValue(datasetVersion != null ? datasetVersion : "")
                                .endEnv()
                                .addNewEnv()
                                    .withName("REQUESTED_BY").withValue(requestedBy)
                                .endEnv()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(k8sJob).create();

        log.info("[K8sJobTrigger] k8s Job 생성 완료 — k8sJobName={}, jobId={}, jobType={}",
                k8sJobName, jobId, jobType);
    }
}
