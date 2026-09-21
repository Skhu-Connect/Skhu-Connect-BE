package org.skhuconnect.petition.similarity.entity;

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
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "petition_embeddings", indexes = {
        @Index(name = "ux_petition_embeddings_petition_id",
                columnList = "petition_id", unique = true),
        @Index(name = "ix_petition_embeddings_status_model",
                columnList = "status, model_name, dimensions"),
        @Index(name = "ix_petition_embeddings_content_hash",
                columnList = "content_hash")
})
public class PetitionEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "petition_id", nullable = false, unique = true)
    private Petition petition;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "dimensions", nullable = false)
    private int dimensions;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Lob
    @Column(name = "embedding", nullable = false)
    private byte[] embedding;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private PetitionEmbeddingStatus status;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    protected PetitionEmbedding() {
    }

    private PetitionEmbedding(Petition petition) {
        this.petition = Objects.requireNonNull(petition, "petition must not be null");
    }

    public static PetitionEmbedding ready(
            Petition petition,
            String modelName,
            int dimensions,
            String contentHash,
            byte[] embedding,
            LocalDateTime embeddedAt
    ) {
        PetitionEmbedding value = new PetitionEmbedding(petition);
        value.updateReady(modelName, dimensions, contentHash, embedding, embeddedAt);
        return value;
    }

    public static PetitionEmbedding failed(
            Petition petition,
            String modelName,
            int dimensions,
            String contentHash,
            String lastError,
            LocalDateTime failedAt
    ) {
        PetitionEmbedding value = new PetitionEmbedding(petition);
        value.updateFailed(modelName, dimensions, contentHash, lastError, failedAt);
        return value;
    }

    public void updateReady(
            String modelName,
            int dimensions,
            String contentHash,
            byte[] embedding,
            LocalDateTime embeddedAt
    ) {
        this.modelName = requireText(modelName, "modelName");
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
        this.contentHash = requireText(contentHash, "contentHash");
        this.embedding = Objects.requireNonNull(embedding, "embedding must not be null").clone();
        this.status = PetitionEmbeddingStatus.READY;
        this.lastError = null;
        this.embeddedAt = Objects.requireNonNull(embeddedAt, "embeddedAt must not be null");
    }

    public void updateFailed(
            String modelName,
            int dimensions,
            String contentHash,
            String lastError,
            LocalDateTime failedAt
    ) {
        this.modelName = requireText(modelName, "modelName");
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
        this.contentHash = requireText(contentHash, "contentHash");
        this.embedding = new byte[0];
        this.status = PetitionEmbeddingStatus.FAILED;
        this.lastError = abbreviate(lastError);
        this.embeddedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
    }

    public void markStale() {
        this.status = PetitionEmbeddingStatus.STALE;
    }

    public boolean isReadyFor(String modelName, int dimensions, String contentHash) {
        return status == PetitionEmbeddingStatus.READY
                && this.dimensions == dimensions
                && this.modelName.equals(modelName)
                && this.contentHash.equals(contentHash);
    }

    public Long getId() {
        return id;
    }

    public Petition getPetition() {
        return petition;
    }

    public String getModelName() {
        return modelName;
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getContentHash() {
        return contentHash;
    }

    public byte[] getEmbedding() {
        return embedding.clone();
    }

    public PetitionEmbeddingStatus getStatus() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getEmbeddedAt() {
        return embeddedAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
