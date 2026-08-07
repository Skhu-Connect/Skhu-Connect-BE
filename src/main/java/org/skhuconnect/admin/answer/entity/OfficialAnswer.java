package org.skhuconnect.admin.answer.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;

import java.util.Objects;

@Entity
@Table(name = "official_answers", indexes = {
        @Index(name = "ux_official_answers_petition_id", columnList = "petition_id", unique = true)
})
public class OfficialAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "petition_id", nullable = false, unique = true)
    private Petition petition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_source", nullable = false, length = 30)
    private AnswerSource answerSource;

    protected OfficialAnswer() {
    }

    private OfficialAnswer(Petition petition, Admin admin, String content, AnswerSource answerSource) {
        this.petition = Objects.requireNonNull(petition, "petition must not be null");
        this.admin = Objects.requireNonNull(admin, "admin must not be null");
        this.content = requireContent(content);
        this.answerSource = Objects.requireNonNull(answerSource, "answerSource must not be null");
    }

    public static OfficialAnswer create(
            Petition petition, Admin admin, String content, AnswerSource answerSource
    ) {
        return new OfficialAnswer(petition, admin, content, answerSource);
    }

    public void update(Admin admin, String content, AnswerSource answerSource) {
        this.admin = Objects.requireNonNull(admin, "admin must not be null");
        this.content = requireContent(content);
        this.answerSource = Objects.requireNonNull(answerSource, "answerSource must not be null");
    }

    public Long getId() { return id; }
    public Petition getPetition() { return petition; }
    public Admin getAdmin() { return admin; }
    public String getContent() { return content; }
    public AnswerSource getAnswerSource() { return answerSource; }

    private static String requireContent(String content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("content must not exceed 1000 characters");
        }
        return content;
    }
}