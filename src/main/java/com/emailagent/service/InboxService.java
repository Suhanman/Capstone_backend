package com.emailagent.service;

import com.emailagent.domain.entity.*;
import com.emailagent.domain.enums.DraftStatus;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.dto.inbox.AttachmentMetaDto;
import com.emailagent.dto.request.inbox.CalendarActionRequest;
import com.emailagent.dto.response.inbox.AttachmentResponseDto;
import com.emailagent.dto.request.inbox.RegenerateRequest;
import com.emailagent.dto.request.inbox.ReplyActionRequest;
import com.emailagent.dto.response.inbox.AttachmentDownloadResult;
import com.emailagent.dto.response.inbox.InboxActionResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse.*;
import com.emailagent.dto.response.inbox.InboxListResponse;
import com.emailagent.dto.response.inbox.RegenerateResponse;
import com.emailagent.exception.CalendarNotConnectedException;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.messaging.EmailMessagePublisher;
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

import static java.util.Collections.emptyList;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final EmailRepository emailRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;
    private final IntegrationRepository integrationRepository;
    private final EmailAnalysisResultRepository emailAnalysisResultRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final EmailMessagePublisher emailMessagePublisher;
    private final BusinessService businessService;
    private final GmailApiService gmailApiService;

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
                .totalElements(emailPage.getTotalElements())
                .content(content)
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

        // attachments_meta → 프론트 전달용 DTO 변환 (gmail_attachment_id 제외)
        List<AttachmentResponseDto> attachments = email.getAttachmentsMeta() != null
                ? email.getAttachmentsMeta().stream()
                        .map(AttachmentResponseDto::from)
                        .toList()
                : List.of();

        EmailInfo emailInfo = EmailInfo.builder()
                .emailId(email.getEmailId())
                .senderName(email.getSenderName())
                .subject(email.getSubject())
                .body(email.getBodyClean())
                .receivedAt(email.getReceivedAt())
                .hasAttachments(email.isHasAttachments())
                .attachments(attachments)
                .build();

        AiAnalysis aiAnalysis = (ar != null) ? AiAnalysis.builder()
                .domain(ar.getDomain())
                .intent(ar.getIntent())
                .summary(ar.getSummaryText())
                .entities(ar.getEntitiesJson())
                .confidenceScore(ar.getConfidenceScore())
                .scheduleDetected(ar.isScheduleDetected())
                .build() : null;

        DraftReplyInfo draftReplyInfo = draftReplyRepository
                .findByEmailIdAndUserId(emailId, userId)
                .map(draft -> buildDraftReplyInfo(draft, ar))
                .orElse(null);

        return InboxDetailResponse.builder()
                .emailInfo(emailInfo)
                .aiAnalysis(aiAnalysis)
                .draftReply(draftReplyInfo)
                .build();
    }

    // =============================================
    // POST /api/inbox/{email_id}/regenerate
    // =============================================

    @Transactional
    public RegenerateResponse regenerate(Long userId, Long emailId, RegenerateRequest request) {
        // 1. emailId로 Email 조회 (본인 소유 검증)
        Email email = findEmailForUser(emailId, userId);

        // 2. EmailAnalysisResults에서 domain, intent, summary 조회
        EmailAnalysisResult ar = emailAnalysisResultRepository.findByEmail_EmailId(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("이메일 분석 결과를 찾을 수 없습니다."));

        // 3. RAG context 생성
        String ragContext = businessService.buildRagContext(userId);

        // 4. BusinessProfile에서 emailTone 조회
        String emailTone = businessProfileRepository.findByUser_UserId(userId)
                .map(p -> p.getEmailTone().name())
                .orElse(null);

        // 5. email.draft 큐로 재생성 요청 발행 (mode="regenerate")
        emailMessagePublisher.publishDraftRequest(
                emailId,
                email.getSubject(),
                email.getBodyClean(),
                ar.getDomain(),
                ar.getIntent(),
                ar.getSummaryText(),
                emailTone,
                ragContext,
                request.getPreviousDraft(),
                "regenerate"
        );

        // 6. DraftReply status → PENDING_REVIEW로 초기화
        draftReplyRepository.findByEmailIdAndUserId(emailId, userId)
                .ifPresent(draft -> draft.updateStatus(DraftStatus.PENDING_REVIEW));

        return new RegenerateResponse("답장 재생성 요청이 접수되었습니다.");
    }

    // =============================================
    // POST /api/inbox/{email_id}/reply
    // =============================================

    @Transactional
    public InboxActionResponse processReply(Long userId, Long emailId, ReplyActionRequest request) {
        Email email = findEmailForUser(emailId, userId);
        DraftReply draft = draftReplyRepository.findByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("초안을 찾을 수 없습니다."));

        String action = request.getAction().toUpperCase();

        String message = switch (action) {
            case "SEND" -> {
                // AI가 생성한 초안 그대로 발송
                // 발송 실패 시 EmailSendFailedException → 트랜잭션 롤백 (상태 변경 취소)
                gmailApiService.sendEmail(
                        userId,
                        email.getSenderEmail(),
                        draft.getDraftSubject() != null ? draft.getDraftSubject() : "Re: " + email.getSubject(),
                        draft.getDraftContent()
                );
                email.updateStatus(EmailStatus.PROCESSED);
                draft.updateStatus(DraftStatus.SENT);
                yield "답장이 발송되었습니다.";
            }
            case "EDIT_SEND" -> {
                if (request.getContent() == null || request.getContent().isBlank()) {
                    throw new IllegalArgumentException("EDIT_SEND 액션은 content가 필요합니다.");
                }
                // 사용자가 수정한 내용으로 발송
                gmailApiService.sendEmail(
                        userId,
                        email.getSenderEmail(),
                        draft.getDraftSubject() != null ? draft.getDraftSubject() : "Re: " + email.getSubject(),
                        request.getContent()
                );
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

        return InboxActionResponse.builder().message(message).build();
    }

    // =============================================
    // POST /api/inbox/{email_id}/calendar
    // =============================================

    @Transactional
    public InboxActionResponse processCalendar(Long userId, Long emailId, CalendarActionRequest request) {
        Email email = findEmailForUser(emailId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        String action = request.getAction().toUpperCase();

        String message = switch (action) {
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

        return InboxActionResponse.builder().message(message).build();
    }

    // =============================================
    // GET /api/inbox/{email_id}/attachments/{attachment_id}
    // 첨부파일 On-demand 다운로드
    // =============================================

    @Transactional(readOnly = true)
    public AttachmentDownloadResult downloadAttachment(Long userId, Long emailId, int attachmentId) {
        // 1. 이메일 조회 — userId로 본인 소유 여부 검증, Gmail messageId(externalMsgId) 확보
        Email email = findEmailForUser(emailId, userId);

        // 2. attachments_meta JSON에서 1-based 시퀀스 attachment_id로 메타데이터 조회
        //    attachment_id(int, 우리 API 식별자) → gmail_attachment_id(String, Gmail API 식별자)
        List<AttachmentMetaDto> metaList = email.getAttachmentsMeta() != null
                ? email.getAttachmentsMeta() : emptyList();

        AttachmentMetaDto meta = metaList.stream()
                .filter(m -> m.getAttachmentId() == attachmentId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("첨부파일을 찾을 수 없습니다."));

        // 3. Gmail API 호출: externalMsgId + gmail_attachment_id(String)로 바이트 취득
        //    gmailApiService 내부에서 Base64URL 디코딩 처리
        byte[] data = gmailApiService.getAttachmentBytes(
                userId,
                email.getExternalMsgId(),
                meta.getGmailAttachmentId()
        );

        // 4. attachments_meta에 저장된 원본 파일명과 MIME 타입을 함께 반환 (응답 헤더 세팅에 사용)
        String mimeType = meta.getContentType() != null
                ? meta.getContentType()
                : "application/octet-stream";

        return new AttachmentDownloadResult(data, meta.getFileName(), mimeType);
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
