package org.skhuconnect.petition.similarity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.util.Objects;

@Entity
@Table(name = "petition_similarity_search_logs", indexes = {
        @Index(name = "ix_similarity_logs_user_created",
                columnList = "user_id, created_at"),
        @Index(name = "ix_similarity_logs_user_query",
                columnList = "user_id, query_hash, model_name, dimensions, created_at")
})
public class PetitionSimilaritySearchLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "dimensions", nullable = false)
    private int dimensions;

    @Lob
    @Column(name = "query_embedding", nullable = false, columnDefinition = "BLOB")
    private byte[] queryEmbedding;

    @Column(name = "counted", nullable = false)
    private boolean counted;

    protected PetitionSimilaritySearchLog() {
    }

    private PetitionSimilaritySearchLog(
            User user,
            String queryHash,
            String modelName,
            int dimensions,
            byte[] queryEmbedding,
            boolean counted
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.queryHash = requireText(queryHash, "queryHash");
        this.modelName = requireText(modelName, "modelName");
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
        this.queryEmbedding = Objects.requireNonNull(
                queryEmbedding, "queryEmbedding must not be null").clone();
        this.counted = counted;
    }

    public static PetitionSimilaritySearchLog counted(
            User user,
            String queryHash,
            String modelName,
            int dimensions,
            byte[] queryEmbedding
    ) {
        return new PetitionSimilaritySearchLog(
                user, queryHash, modelName, dimensions, queryEmbedding, true);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getQueryHash() {
        return queryHash;
    }

    public String getModelName() {
        return modelName;
    }

    public int getDimensions() {
        return dimensions;
    }

    public byte[] getQueryEmbedding() {
        return queryEmbedding.clone();
    }

    public boolean isCounted() {
        return counted;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
