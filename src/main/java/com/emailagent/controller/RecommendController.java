package com.emailagent.controller;

import com.emailagent.dto.response.recommend.RecommendedDraftResponse;
import com.emailagent.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * GET /api/emails/{emailId}/recommendations?topK=3
     * 이메일의 임베딩 벡터 기준 유사한 초안(DraftReply) topK개 추천
     */
    @GetMapping("/{emailId}/recommendations")
    public ResponseEntity<RecommendedDraftResponse> getRecommendations(
            @PathVariable Long emailId,
            @RequestParam(defaultValue = "3") int topK) {

        RecommendedDraftResponse response = recommendService.recommendSimilarDrafts(emailId, topK);
        return ResponseEntity.ok(response);
    }
}
