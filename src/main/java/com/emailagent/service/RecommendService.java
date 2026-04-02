package com.emailagent.service;

import com.emailagent.converter.VectorConverter;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.dto.response.recommend.RecommendedDraftResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.DraftReplyRepository;
import com.emailagent.repository.EmailAnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final EmailAnalysisResultRepository analysisResultRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final VectorConverter vectorConverter;

    /**
     * emailId의 email_embedding을 기준으로 reply_embedding과 코사인 유사도가 높은 초안 topK개 반환
     * 1. emailId로 EmailAnalysisResult 조회 → email_embedding(float[]) 획득
     * 2. email_embedding null이면 빈 배열 반환
     * 3. float[] → byte[] 변환 (MariaDB VEC_DISTANCE_COSINE 파라미터)
     * 4. 네이티브 쿼리로 유사도 상위 topK 초안 조회
     * 5. RecommendedDraftResponse로 변환
     */
    @Transactional(readOnly = true)
    public RecommendedDraftResponse recommendSimilarDrafts(Long emailId, int topK) {
        // 1. 이메일 분석 결과 조회
        EmailAnalysisResult analysisResult = analysisResultRepository
                .findByEmail_EmailId(emailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "이메일 분석 결과를 찾을 수 없습니다: emailId=" + emailId));

        float[] emailEmbedding = analysisResult.getEmailEmbedding();

        // 2. email_embedding이 없으면 빈 배열 반환
        if (emailEmbedding == null) {
            log.warn("email_embedding이 없습니다: emailId={}", emailId);
            return new RecommendedDraftResponse(List.of());
        }

        // 3. float[] → byte[] 변환 (VectorConverter 활용)
        byte[] embeddingBytes = vectorConverter.convertToDatabaseColumn(emailEmbedding);

        // 4. 유사도 상위 topK 초안 조회
        List<Object[]> rows = draftReplyRepository.findTopKSimilarDrafts(embeddingBytes, topK);

        // 5. 결과를 DraftItem 목록으로 변환
        List<RecommendedDraftResponse.DraftItem> items = rows.stream()
                .map(row -> new RecommendedDraftResponse.DraftItem(
                        ((Number) row[0]).longValue(),   // draft_reply_id
                        (String) row[1],                  // draft_subject
                        (String) row[2],                  // draft_content
                        ((Number) row[3]).doubleValue(),  // similarity
                        ((Number) row[4]).longValue()     // email_id
                ))
                .toList();

        log.info("유사 초안 추천 완료: emailId={}, 결과 수={}", emailId, items.size());
        return new RecommendedDraftResponse(items);
    }
}
