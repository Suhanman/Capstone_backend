package com.emailagent.domain.entity;

import com.emailagent.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "color", length = 30)
    private String color;

    @Convert(converter = StringListConverter.class)
    @Column(name = "keywords", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Template> templates = new ArrayList<>();

    public List<String> getKeywords() {
        return keywords == null ? Collections.emptyList() : keywords;
    }

    public void updateKeywordsByAdmin(String color, List<String> keywords) {
        this.color = color;
        this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords);
    }
}
