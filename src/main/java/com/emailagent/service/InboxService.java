package com.emailagent.service;

import com.emailagent.domain.entity.*;
import com.emailagent.domain.enums.DraftStatus;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.dto.request.inbox.CalendarActionRequest;
import com.emailagent.dto.request.inbox.ReplyActionRequest;
import com.emailagent.dto.response.inbox.InboxDetailResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse.*;
import com.emailagent.dto.response.inbox.InboxListResponse;
import com.emailagent.exception.CalendarNotConnectedException;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final EmailRepository emailRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;
    private final IntegrationRepository integrationRepository;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    // =============================================
    // GET /api/inbox
    // =============================================

    @Transactional(readOnly = true)
    public InboxListResponse getInbox(Long userId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Email> emailPage = (status != null)
                ? emailRepository.findInboxPageByStatus(userId, EmailStatus.valueOf(status), pageable)
                : emailRepository.findInboxPage(userId, pageable);

        List<InboxListResponse.EmailItem> content = emailPage.getContent()
                .stream()
                .map(InboxListResponse.EmailItem::from)
                .toList();

        return InboxListResponse.builder()
                .success(true)
                .data(InboxListResponse.InboxPage.builder()
                        .totalElements(emailPage.getTotalElements())
                        .content(content)
                        .build())
                .build();
    }

    // =============================================
    // GET /api/inbox/{email_id}
    // =============================================

    @Transactional(readOnly = true)
    public InboxDetailResponse getDetail(Long userId, Long emailId) {
        Email email = emailRepository.findDetailByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("이메일을 찾을 수 없습니다."));

        EmailAnalysisResult ar = email.getAnalysisResult();

        EmailInfo emailInfo = EmailInfo.builder()
                .emailId(email.getEmailId())
                .senderName(email.getSenderName())
                .subject(email.getSubject())
                .body(email.getBodyClean())
                .receivedAt(email.getReceivedAt())
                .build();

        AiAnalysis aiAnalysis = (ar != null) ? AiAnalysis.builder()
                .domain(ar.getDomain())
                .intent(ar.getIntent())
                .summary(ar.getSummaryText())
                .entities(ar.getEntitiesJson())
                .build() : null;

        DraftReplyInfo draftReplyInfo = draftReplyRepository
                .findByEmailIdAndUserId(emailId, userId)
                .map(draft -> buildDraftReplyInfo(draft, ar))
                .orElse(null);

        return InboxDetailResponse.builder()
                .success(true)
                .data(DetailData.builder()
                        .emailInfo(emailInfo)
                        .aiAnalysis(aiAnalysis)
                        .draftReply(draftReplyInfo)
                        .build())
                .build();
    }

    // =============================================
    // POST /api/inbox/{email_id}/reply
    // =============================================

    @Transactional
    public String processReply(Long userId, Long emailId, ReplyActionRequest request) {
        Email email = findEmailForUser(emailId, userId);
        DraftReply draft = draftReplyRepository.findByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("초안을 찾을 수 없습니다."));

        String action = request.getAction().toUpperCase();

        return switch (action) {
            case "SEND" -> {
                // TODO: Gmail API로 draft.getDraftContent() 발송 (Google OAuth 팀 담당)
                log.info("[TODO] Gmail 발송 - emailId={}, body={}", emailId, draft.getDraftContent());
                email.updateStatus(EmailStatus.PROCESSED);
                draft.updateStatus(DraftStatus.SENT);
                yield "답장이 발송되었습니다.";
            }
            case "EDIT_SEND" -> {
                if (request.getContent() == null || request.getContent().isBlank()) {
                    throw new IllegalArgumentException("EDIT_SEND 액션은 content가 필요합니다.");
                }
                // TODO: Gmail API로 request.getContent() 발송 (Google OAuth 팀 담당)
                log.info("[TODO] Gmail 수정 발송 - emailId={}, body={}", emailId, request.getContent());
                email.updateStatus(EmailStatus.PROCESSED);
                draft.updateStatus(DraftStatus.EDITED);
                yield "수정된 답장이 발송되었습니다.";
            }
            case "SKIP" -> {
                email.updateStatus(EmailStatus.PROCESSED);
                draft.updateStatus(DraftStatus.SKIPPED);
                yield "답장이 건너뛰어졌습니다.";
            }
            default -> throw new IllegalArgumentException("알 수 없는 action: " + request.getAction());
        };
    }

    // =============================================
    // POST /api/inbox/{email_id}/calendar
    // =============================================

    @Transactional
    public String processCalendar(Long userId, Long emailId, CalendarActionRequest request) {
        Email email = findEmailForUser(emailId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        String action = request.getAction().toUpperCase();

        return switch (action) {
            case "ADD" -> {
                // 캘린더 연동 여부 검증 — is_calendar_connected=false 이면 비즈니스 예외
                integrationRepository.findByUser_UserId(userId)
                        .filter(i -> i.isCalendarConnected())
                        .orElseThrow(CalendarNotConnectedException::new);

                CalendarActionRequest.EventDetails details = request.getEventDetails();
                if (details == null || details.getTitle() == null) {
                    throw new IllegalArgumentException("ADD 액션은 event_details.title이 필요합니다.");
                }
                CalendarEvent event = CalendarEvent.builder()
                        .user(user)
                        .email(email)
                        .title(details.getTitle())
                        .startDatetime(details.getStartDatetime())
                        .endDatetime(details.getEndDatetime())
                        .source("EMAIL")
                        .status("CONFIRMED")
                        .isCalendarAdded(true)
                        .build();
                calendarEventRepository.save(event);
                yield "일정이 등록되었습니다.";
            }
            case "IGNORE" -> {
                // 해당 이메일에 연결된 PENDING 일정이 있으면 IGNORED로 변경
                calendarEventRepository
                        .findByEmail_EmailIdAndUser_UserIdAndStatus(emailId, userId, "PENDING")
                        .ifPresent(event -> event.updateStatus("IGNORED"));
                yield "일정이 무시되었습니다.";
            }
            default -> throw new IllegalArgumentException("알 수 없는 action: " + request.getAction());
        };
    }

    // =============================================
    // private helpers
    // =============================================

    private Email findEmailForUser(Long emailId, Long userId) {
        return emailRepository.findDetailByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("이메일을 찾을 수 없습니다."));
    }

    private DraftReplyInfo buildDraftReplyInfo(DraftReply draft, EmailAnalysisResult ar) {
        Template template = draft.getTemplate();

        TemplateInfo templateInfo = (template != null) ? TemplateInfo.builder()
                .templateId(template.getTemplateId())
                .templateTitle(template.getTitle())
                .build() : null;

        VariableInfo variableInfo = buildVariableInfo(template, ar);

        return DraftReplyInfo.builder()
                .draftId(draft.getDraftReplyId())
                .status(draft.getStatus().name())
                .templateInfo(templateInfo)
                .variables(variableInfo)
                .subject(draft.getDraftSubject())
                .body(draft.getDraftContent())
                .build();
    }

    /**
     * 템플릿 플레이스홀더 {{key}} 를 분석해 자동완성/입력필요 변수를 구분한다.
     * entitiesJson에 있으면 auto_completed, 없으면 required_input.
     */
    private VariableInfo buildVariableInfo(Template template, EmailAnalysisResult ar) {
        if (template == null) {
            return VariableInfo.builder()
                    .autoCompletedCount(0).autoCompletedKeys(List.of())
                    .requiredInputCount(0).requiredInputKeys(List.of())
                    .build();
        }

        // bodyTemplate + subjectTemplate 에서 모든 변수명 추출 (중복 제거)
        String combinedTemplate = Optional.ofNullable(template.getBodyTemplate()).orElse("")
                + Optional.ofNullable(template.getSubjectTemplate()).orElse("");
        Set<String> allKeys = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(combinedTemplate);
        while (matcher.find()) {
            allKeys.add(matcher.group(1));
        }

        Map<String, Object> entities = (ar != null && ar.getEntitiesJson() != null)
                ? ar.getEntitiesJson() : Map.of();

        List<String> autoKeys = allKeys.stream()
                .filter(k -> entities.containsKey(k) && entities.get(k) != null)
                .collect(Collectors.toList());
        List<String> requiredKeys = allKeys.stream()
                .filter(k -> !entities.containsKey(k) || entities.get(k) == null)
                .collect(Collectors.toList());

        return VariableInfo.builder()
                .autoCompletedCount(autoKeys.size())
                .autoCompletedKeys(autoKeys)
                .requiredInputCount(requiredKeys.size())
                .requiredInputKeys(requiredKeys)
                .build();
    }
}
