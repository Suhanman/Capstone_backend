package com.emailagent.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fabric8 KubernetesClient 빈 등록.
 *
 * [동작 방식]
 * - k8s 클러스터 내부 실행 시: 서비스 어카운트 토큰(/var/run/secrets/kubernetes.io/serviceaccount/)을 자동 감지
 * - 로컬 개발 환경: ~/.kube/config 를 자동으로 읽어 연결
 * - 별도 설정 없이 KubernetesClientBuilder가 환경을 스스로 판단함
 *
 * [RBAC 주의사항]
 * 클러스터 내부에서 Job 리소스를 생성하려면 서비스 어카운트에
 * batch/jobs 리소스의 create 권한이 있어야 함 (Terraform/인프라 팀 담당)
 */
@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
