package com.emailagent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "AutomationRules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 카테고리 삭제 금지 → nullable=false, cascade 없음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 템플릿이 없을 수 있음 (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(name = "auto_send_enabled", nullable = false)
    @Builder.Default
    private boolean autoSendEnabled = false;

    @Column(name = "auto_calendar_enabled", nullable = false)
    @Builder.Default
    private boolean autoCalendarEnabled = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── 비즈니스 메서드 ──────────────────────────────────────────────────────────

    public void update(Category category, Template template, boolean autoSendEnabled) {
        this.category = category;
        this.template = template;
        this.autoSendEnabled = autoSendEnabled;
    }

    public void toggleAutoSend(boolean enabled) {
        this.autoSendEnabled = enabled;
    }

    public void toggleAutoCalendar(boolean enabled) {
        this.autoCalendarEnabled = enabled;
    }

    // 관리자 PATCH: 선택 필드만 업데이트 (null이면 변경 안 함)
    public void updateByAdmin(Template template,
                               Boolean isActive, Boolean autoSendEnabled) {
        if (template != null) {
            this.template = template;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
        if (autoSendEnabled != null) {
            this.autoSendEnabled = autoSendEnabled;
        }
    }
}
