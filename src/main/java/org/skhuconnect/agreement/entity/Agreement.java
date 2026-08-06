package org.skhuconnect.agreement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;

import java.util.Objects;

@Entity
@Table(
        name = "agreements",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_agreements_petition_user",
                columnNames = {"petition_id", "user_id"}
        ),
        indexes = @Index(
                name = "ix_agreements_user_id_created_at",
                columnList = "user_id, created_at"
        )
)
public class Agreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "petition_id", nullable = false)
    private Petition petition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Agreement() {
    }

    private Agreement(Petition petition, User user) {
        this.petition = Objects.requireNonNull(
                petition, "petition must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
    }

    public static Agreement create(Petition petition, User user) {
        return new Agreement(petition, user);
    }

    public Long getId() {
        return id;
    }

    public Petition getPetition() {
        return petition;
    }

    public User getUser() {
        return user;
    }
}