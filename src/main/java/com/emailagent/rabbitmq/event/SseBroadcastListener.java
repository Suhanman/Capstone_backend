package com.emailagent.rabbitmq.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * SSE Pod 브로드캐스트 리스너.
 *
 * markFinished() 트랜잭션이 커밋된 이후(AFTER_COMMIT)에만 동작하여
 * DB 저장 실패 시 SSE 알림이 나가지 않도록 보장한다.
 *
 * APP Pod → 모든 SSE Pod에 HTTP POST /internal/sse/push 브로드캐스트.
 * SSE Pod는 해당 userId의 emitter가 있으면 send, 없으면 무시.
 */
@Slf4j
@Component
public class SseBroadcastListener {

    private final List<String> ssePodUrls;
    private final WebClient webClient;

    public SseBroadcastListener(
            @Value("${app.sse.pods}") List<String> ssePodUrls,
            WebClient.Builder webClientBuilder) {
        this.ssePodUrls = ssePodUrls;
        this.webClient = webClientBuilder.build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSseNotify(SseNotifyEvent event) {
        Long emailId = event.getEmailId();
        Map<String, Object> body = Map.of("email_id", emailId);

        // 모든 SSE Pod에 비동기 브로드캐스트 (subscribe → fire-and-forget)
        for (String podUrl : ssePodUrls) {
            webClient.post()
                    .uri(podUrl + "/internal/sse/push")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            resp -> log.debug("[SSE Broadcast] 성공 — pod={}, emailId={}", podUrl, emailId),
                            err  -> log.warn("[SSE Broadcast] 실패 — pod={}, emailId={}, error={}", podUrl, emailId, err.getMessage())
                    );
        }
    }
}
