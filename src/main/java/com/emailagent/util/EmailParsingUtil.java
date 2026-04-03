package com.emailagent.util;

import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Gmail 메시지 파싱 및 텍스트 정제 유틸리티 (6단계 파이프라인).
 * 모든 메서드는 static — 인스턴스화 불필요.
 *
 * 단계별 역할:
 * 1~2단계: parseHeaders()      — Gmail headers(From/To/Subject/Date 등) 추출
 * 3단계:   extractBodyRaw()    — text/plain 우선 재귀 탐색, 없으면 text/html (attachment 제외)
 * 4단계:   extractBodyRaw()    — Base64URL 디코딩 포함
 * 5단계:   cleanBody()         — HTML 태그 제거(Jsoup), 서명·인용문 제거, 공백 정규화
 * 6단계:   PubSubHandlerService에서 JSON 규격 구성
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailParsingUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // RFC 3676: 서명 구분선 — "-- " 단독 줄
    private static final Pattern SIG_DELIMITER = Pattern.compile("(?m)^--\\s*$");

    // 인용문: ">" 로 시작하는 줄
    private static final Pattern QUOTE_LINE = Pattern.compile("(?m)^>+.*$");

    // Reply/Forward 헤더 줄
    private static final Pattern REPLY_HEADER = Pattern.compile(
            "(?im)^(on .+wrote:|-----\\s*original message\\s*-----|-{3,}\\s*forwarded message\\s*-{3,}|보낸\\s*사람\\s*:.*)$");

    // "Sent from my iPhone", "Get Outlook for Android" 계열 footer
    private static final Pattern COMMON_FOOTER = Pattern.compile(
            "(?im)^(sent from my .+|get outlook for .+|이 메일은 .+에서 보냈습니다)$");

    // 3개 이상 연속 공백/빈줄
    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("[ \\t]{2,}|\\n{3,}");

    // ── 1~2단계: 헤더 파싱 ──────────────────────────────────────────────────────

    /**
     * Gmail MessagePartHeader 리스트를 이름→값 Map으로 변환한다.
     * 동일 헤더명이 여러 번 나오면 첫 번째 값만 유지한다.
     */
    public static Map<String, String> parseHeaders(List<MessagePartHeader> headers) {
        Map<String, String> map = new LinkedHashMap<>();
        if (headers == null) return map;
        for (MessagePartHeader h : headers) {
            map.putIfAbsent(h.getName(), h.getValue());
        }
        return map;
    }

    /** "홍길동 <user@gmail.com>" 형식에서 이름만 추출 */
    public static String extractSenderName(String from) {
        if (from == null) return "";
        int lt = from.indexOf('<');
        return lt > 0 ? from.substring(0, lt).trim().replace("\"", "") : "";
    }

    /** "홍길동 <user@gmail.com>" 또는 "user@gmail.com" 에서 이메일만 추출 */
    public static String extractSenderEmail(String from) {
        if (from == null) return "";
        int lt = from.indexOf('<');
        int gt = from.indexOf('>');
        if (lt >= 0 && gt > lt) return from.substring(lt + 1, gt).trim();
        return from.trim();
    }

    /**
     * Date 헤더(RFC 2822)를 ISO-8601 오프셋 문자열로 변환한다.
     * 파싱 실패 시 internalDate(ms)를 KST 기준으로 변환하여 반환한다.
     */
    public static String formatEmailDate(String dateHeader, Long internalDateMs) {
        if (dateHeader != null && !dateHeader.isBlank()) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(
                        dateHeader.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                log.debug("[EmailParsing] Date 헤더 파싱 실패, internalDate fallback — dateHeader={}", dateHeader);
            }
        }
        ZonedDateTime fallback = internalDateMs != null
                ? ZonedDateTime.ofInstant(Instant.ofEpochMilli(internalDateMs), KST)
                : ZonedDateTime.now(KST);
        return fallback.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // ── 3~4단계: 본문 추출 (Base64URL 디코딩 포함) ─────────────────────────────

    /**
     * text/plain 우선 탐색 후 없으면 text/html 탐색.
     * attachment 파트(filename 있음)는 재귀 탐색에서 제외한다.
     *
     * @return Base64URL 디코딩된 원본 텍스트(또는 HTML)
     */
    public static String extractBodyRaw(MessagePart root) {
        String plain = extractPart(root, "text/plain");
        if (!plain.isBlank()) return plain;
        return extractPart(root, "text/html");
    }

    /**
     * text/plain이 존재하면 false, text/html fallback이면 true를 반환한다.
     * cleanBody() 호출 시 isHtml 인자 결정에 사용한다.
     */
    public static boolean isHtmlBody(MessagePart root) {
        return extractPart(root, "text/plain").isBlank();
    }

    private static String extractPart(MessagePart part, String targetMime) {
        if (part == null) return "";
        // attachment 파트 제외 (파일명 있는 파트)
        if (part.getFilename() != null && !part.getFilename().isBlank()) return "";

        if (targetMime.equals(part.getMimeType())
                && part.getBody() != null
                && part.getBody().getData() != null) {
            // 4단계: Base64URL 디코딩
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart sub : part.getParts()) {
                String result = extractPart(sub, targetMime);
                if (!result.isBlank()) return result;
            }
        }
        return "";
    }

    // ── 5단계: 텍스트 정제 ─────────────────────────────────────────────────────

    /**
     * 5단계 텍스트 정제 파이프라인:
     * ① HTML이면 Jsoup으로 태그 제거
     * ② RFC 3676 서명 구분선("-- ") 이후 내용 제거
     * ③ ">" 인용문 줄 제거
     * ④ Reply/Forward 헤더 줄 제거
     * ⑤ "Sent from my iPhone" 계열 footer 제거
     * ⑥ 연속 공백·빈줄 정규화
     *
     * @param raw    extractBodyRaw()로 얻은 원본 텍스트 또는 HTML
     * @param isHtml HTML 여부 (isHtmlBody()로 판별)
     */
    public static String cleanBody(String raw, boolean isHtml) {
        if (raw == null || raw.isBlank()) return "";

        // ① HTML 태그 제거 (Jsoup)
        String text = isHtml ? Jsoup.parse(raw).text() : raw;

        // ② 서명 구분선("-- ") 이후 제거
        java.util.regex.Matcher sigMatcher = SIG_DELIMITER.matcher(text);
        if (sigMatcher.find()) {
            text = text.substring(0, sigMatcher.start());
        }

        // ③ 인용문 줄 제거 ("> " 시작)
        text = QUOTE_LINE.matcher(text).replaceAll("");

        // ④ Reply/Forward 헤더 줄 제거
        text = REPLY_HEADER.matcher(text).replaceAll("");

        // ⑤ 흔한 footer 문구 제거
        text = COMMON_FOOTER.matcher(text).replaceAll("");

        // ⑥ 연속 공백·빈줄 정규화
        text = EXCESSIVE_WHITESPACE.matcher(text).replaceAll(m -> m.group().contains("\n") ? "\n" : " ").trim();

        return text;
    }

    // ── 첨부파일 파트 수집 ──────────────────────────────────────────────────────

    /**
     * Gmail 메시지 파트 트리를 재귀 탐색하여 첨부파일 파트 목록을 반환한다.
     * 파일명이 있는 파트를 첨부파일로 간주한다.
     */
    public static List<MessagePart> collectAttachmentParts(MessagePart root) {
        List<MessagePart> result = new ArrayList<>();
        collectRecursive(root, result);
        return result;
    }

    private static void collectRecursive(MessagePart part, List<MessagePart> acc) {
        if (part == null) return;
        if (part.getFilename() != null && !part.getFilename().isBlank()) {
            acc.add(part);
            return; // 첨부파일 파트 아래로는 더 탐색하지 않음
        }
        if (part.getParts() != null) {
            for (MessagePart sub : part.getParts()) {
                collectRecursive(sub, acc);
            }
        }
    }
}
