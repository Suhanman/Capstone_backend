package com.emailagent.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class CategoryKeywordDefaults {

    private static final Map<String, List<String>> KEYWORDS_BY_CATEGORY = Map.ofEntries(
            Map.entry("견적 요청", List.of("견적", "가격", "도입", "비용", "제안")),
            Map.entry("계약 문의", List.of("계약", "계약서", "검토", "서명", "조건")),
            Map.entry("가격 협상", List.of("가격", "할인", "협상", "견적", "단가")),
            Map.entry("제안서 요청", List.of("제안서", "제안", "자료", "소개서", "RFP")),
            Map.entry("미팅 일정 조율", List.of("미팅", "회의", "일정", "시간", "장소")),
            Map.entry("협찬/제휴 제안", List.of("협찬", "제휴", "파트너십", "협업", "브랜드")),
            Map.entry("광고 문의", List.of("광고", "캠페인", "매체", "집행", "단가")),
            Map.entry("보도자료 요청", List.of("보도자료", "기사", "언론", "배포", "홍보")),
            Map.entry("인터뷰 요청", List.of("인터뷰", "취재", "질문", "일정", "매체")),
            Map.entry("콘텐츠 협업 문의", List.of("콘텐츠", "협업", "콜라보", "제작", "채널")),
            Map.entry("행사/캠페인 문의", List.of("행사", "캠페인", "이벤트", "참여", "운영")),
            Map.entry("채용 문의", List.of("채용", "지원", "공고", "포지션", "면접")),
            Map.entry("면접 일정 조율", List.of("면접", "일정", "시간", "후보자", "조율")),
            Map.entry("휴가 신청", List.of("휴가", "연차", "반차", "신청", "승인")),
            Map.entry("증명서 발급 요청", List.of("증명서", "발급", "재직", "경력", "서류")),
            Map.entry("세금계산서 요청", List.of("세금계산서", "계산서", "사업자", "발행", "청구")),
            Map.entry("비용 처리 문의", List.of("비용", "처리", "정산", "영수증", "지출")),
            Map.entry("입금 확인 요청", List.of("입금", "결제", "확인", "송금", "대금")),
            Map.entry("정산 문의", List.of("정산", "금액", "내역", "지급", "마감")),
            Map.entry("불만 접수", List.of("불만", "클레임", "불편", "항의", "문제")),
            Map.entry("기술 지원 요청", List.of("기술지원", "오류", "장애", "문의", "해결")),
            Map.entry("환불 요청", List.of("환불", "취소", "반품", "결제", "처리")),
            Map.entry("사용법 문의", List.of("사용법", "가이드", "문의", "설정", "도움")),
            Map.entry("시스템 오류 보고", List.of("오류", "장애", "시스템", "버그", "복구")),
            Map.entry("계정 생성 요청", List.of("계정", "생성", "가입", "사용자", "접근")),
            Map.entry("권한 변경 요청", List.of("권한", "변경", "접근", "승인", "관리자")),
            Map.entry("공지 전달", List.of("공지", "안내", "전달", "알림", "공유")),
            Map.entry("내부 보고", List.of("보고", "공유", "현황", "진행", "결과")),
            Map.entry("자료 요청", List.of("자료", "요청", "파일", "문서", "공유")),
            Map.entry("협조 요청", List.of("협조", "요청", "지원", "확인", "처리"))
    );

    private CategoryKeywordDefaults() {
    }

    public static List<String> resolve(String categoryName, List<String> keywords) {
        List<String> normalizedKeywords = normalize(keywords);
        if (!normalizedKeywords.isEmpty()) {
            return normalizedKeywords;
        }

        List<String> defaults = KEYWORDS_BY_CATEGORY.get(categoryName);
        if (defaults != null && !defaults.isEmpty()) {
            return defaults;
        }

        return normalize(List.of(categoryName.split("[\\s/]+")));
    }

    public static List<String> normalize(List<String> keywords) {
        if (keywords == null) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> uniqueKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                uniqueKeywords.add(keyword.trim());
            }
        }
        return new ArrayList<>(uniqueKeywords);
    }
}
