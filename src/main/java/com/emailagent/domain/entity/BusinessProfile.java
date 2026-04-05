package com.emailagent.domain.entity;

import com.emailagent.domain.enums.EmailTone;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "industry_type", length = 100)
    private String industryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_tone")
    @Builder.Default
    private EmailTone emailTone = EmailTone.NEUTRAL;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Upsert용 업데이트 메서드
    public void update(String industryType, EmailTone emailTone, String companyDescription) {
        this.industryType = industryType;
        this.emailTone = emailTone;
        this.companyDescription = companyDescription;
    }
}
