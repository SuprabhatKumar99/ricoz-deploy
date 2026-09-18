package com.ricozknow.article;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.common.TenantOwnedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "article_versions")
@Getter
@Setter
@NoArgsConstructor
public class ArticleVersion extends TenantOwnedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    /**
     * Structured JSON content blocks.
     *
     * Stored directly as PostgreSQL JSONB.
     * Raw/executable HTML is not allowed.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode content;

    @Column(name = "change_summary")
    private String changeSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Published versions are immutable.
     */
    @Transient
    public boolean isImmutable() {
        return status == VersionStatus.PUBLISHED;
    }
}