package org.skhuconnect.petition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "petitions", indexes = {
        @Index(name = "ix_petitions_status_created_at", columnList = "status, created_at"),
        @Index(name = "ix_petitions_category_created_at", columnList = "category, created_at"),
        @Index(name = "ix_petitions_hidden_deleted", columnList = "hidden, deleted"),
        @Index(name = "ix_petitions_writer_id_created_at", columnList = "writer_id, created_at"),
        @Index(name = "ix_petitions_status_agreement_deadline",
                columnList = "status, agreement_deadline")
})
public class Petition extends BaseEntity {

    private static final int AGREEMENT_PERIOD_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 30)
    private PetitionCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private PetitionStatus status;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "agreement_count", nullable = false)
    @ColumnDefault("0")
    private int agreementCount;

    @Column(name = "target_agreement_count", nullable = false)
    private int targetAgreementCount;

    @Column(name = "agreement_deadline", nullable = false)
    private LocalDateTime agreementDeadline;

    @Column(name = "review_started_at")
    private LocalDateTime reviewStartedAt;

    @Column(name = "hidden", nullable = false)
    @ColumnDefault("false")
    private boolean hidden;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "deleted", nullable = false)
    @ColumnDefault("false")
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Petition() {
    }

    private Petition(
            User writer,
            PetitionCategory category,
            String title,
            String content,
            int targetAgreementCount,
            LocalDateTime createdAt
    ) {
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.title = requireText(title, "title");
        this.content = requireText(content, "content");
        if (targetAgreementCount <= 0) {
            throw new IllegalArgumentException("targetAgreementCount must be positive");
        }
        LocalDateTime creationTime = Objects.requireNonNull(
                createdAt, "createdAt must not be null");
        this.status = PetitionStatus.OPEN;
        this.agreementCount = 0;
        this.targetAgreementCount = targetAgreementCount;
        this.agreementDeadline = creationTime.plusDays(AGREEMENT_PERIOD_DAYS);
        this.hidden = false;
        this.deleted = false;
    }

    public static Petition create(
            User writer,
            PetitionCategory category,
            String title,
            String content,
            int targetAgreementCount,
            LocalDateTime createdAt
    ) {
        return new Petition(writer, category, title, content,
                targetAgreementCount, createdAt);
    }

    public void update(String title, String content) {
        validateEditable();
        this.title = requireText(title, "title");
        this.content = requireText(content, "content");
    }

    public void delete(LocalDateTime deletedAt) {
        validateEditable();
        this.deleted = true;
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }

    public boolean isAgreementOpenAt(LocalDateTime now) {
        return status == PetitionStatus.OPEN
                && agreementDeadline.isAfter(
                        Objects.requireNonNull(now, "now must not be null"))
                && !hidden
                && !deleted;
    }

    public void addAgreement(LocalDateTime agreedAt) {
        if (!isAgreementOpenAt(agreedAt)) {
            throw new IllegalStateException("petition is not agreeable");
        }
        agreementCount++;
        if (agreementCount >= targetAgreementCount) {
            status = PetitionStatus.UNDER_REVIEW;
            reviewStartedAt = agreedAt;
        }
    }

    public void removeAgreement(LocalDateTime canceledAt) {
        if (!isAgreementOpenAt(canceledAt) || agreementCount <= 0) {
            throw new IllegalStateException("petition agreement cannot be canceled");
        }
        agreementCount--;
    }
    public boolean isWrittenBy(Long userId) {
        return writer.getId().equals(userId);
    }

    public boolean isEditable() {
        return status == PetitionStatus.OPEN && agreementCount == 0 && !hidden && !deleted;
    }

    public Long getId() {
        return id;
    }

    public User getWriter() {
        return writer;
    }

    public PetitionCategory getCategory() {
        return category;
    }

    public PetitionStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getAgreementCount() {
        return agreementCount;
    }

    public int getTargetAgreementCount() {
        return targetAgreementCount;
    }

    public LocalDateTime getAgreementDeadline() {
        return agreementDeadline;
    }

    public LocalDateTime getReviewStartedAt() {
        return reviewStartedAt;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getHiddenReason() {
        return hiddenReason;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private void validateEditable() {
        if (!isEditable()) {
            throw new IllegalStateException("petition is not editable");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
