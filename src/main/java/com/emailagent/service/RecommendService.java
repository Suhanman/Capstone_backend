package com.emailagent.service;

import com.emailagent.domain.entity.EmailTemplateRecommendation;
import com.emailagent.dto.response.recommend.RecommendedDraftResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.EmailAnalysisResultRepository;
import com.emailagent.repository.EmailTemplateRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final EmailTemplateRecommendationRepository recommendationRepository;
    private final EmailAnalysisResultRepository analysisResultRepository;

    /**
     * 현재 추천 API는 RAG templates.match 결과를 저장한 값을 반환한다.
     * 백엔드는 더 이상 email/reply 임베딩을 직접 비교하지 않는다.
     */
    @Transactional(readOnly = true)
    public RecommendedDraftResponse recommendSimilarDrafts(Long emailId, int topK) {
        List<EmailTemplateRecommendation> storedRecommendations = recommendationRepository
                .findByUserIdAndEmailIdOrderByRank(resolveUserId(emailId), emailId);
        if (!storedRecommendations.isEmpty()) {
            List<RecommendedDraftResponse.DraftItem> items = storedRecommendations.stream()
                    .limit(topK)
                    .map(recommendation -> new RecommendedDraftResponse.DraftItem(
                            recommendation.getTemplate().getTemplateId(),
                            recommendation.getTemplate().getSubjectTemplate(),
                            recommendation.getTemplate().getBodyTemplate(),
                            recommendation.getScore(),
                            emailId,
                            recommendation.getTemplate().getTitle()
                    ))
                    .toList();

            log.info("RAG template match 추천 반환: emailId={}, 결과 수={}", emailId, items.size());
            return new RecommendedDraftResponse(items);
        }
        log.info("저장된 RAG template match 결과가 없습니다: emailId={}", emailId);
        return new RecommendedDraftResponse(List.of());
    }

    private Long resolveUserId(Long emailId) {
        return analysisResultRepository.findByEmail_EmailId(emailId)
                .map(result -> result.getEmail().getUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("이메일 분석 결과를 찾을 수 없습니다: emailId=" + emailId));
    }
}
