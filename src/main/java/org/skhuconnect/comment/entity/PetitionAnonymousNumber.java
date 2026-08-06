package org.skhuconnect.comment.entity;

import jakarta.persistence.Column;
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
        name = "petition_anonymous_numbers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_petition_anonymous_numbers_petition_user",
                        columnNames = {"petition_id", "user_id"}
                ),
                @UniqueConstraint(
                        name = "ux_petition_anonymous_numbers_petition_number",
                        columnNames = {"petition_id", "anonymous_number"}
                )
        },
        indexes = @Index(
                name = "ix_petition_anonymous_numbers_user_id",
                columnList = "user_id"
        )
)
public class PetitionAnonymousNumber extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "petition_id", nullable = false)
    private Petition petition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "anonymous_number", nullable = false)
    private int anonymousNumber;

    protected PetitionAnonymousNumber() {
    }

    private PetitionAnonymousNumber(Petition petition, User user, int anonymousNumber) {
        this.petition = Objects.requireNonNull(petition, "petition must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        if (anonymousNumber <= 0) {
            throw new IllegalArgumentException("anonymousNumber must be positive");
        }
        this.anonymousNumber = anonymousNumber;
    }

    public static PetitionAnonymousNumber create(
            Petition petition,
            User user,
            int anonymousNumber
    ) {
        return new PetitionAnonymousNumber(petition, user, anonymousNumber);
    }

    public boolean matches(Petition petition, User user) {
        return sameEntity(this.petition, petition) && sameEntity(this.user, user);
    }

    private boolean sameEntity(Object stored, Object candidate) {
        if (stored == candidate) {
            return true;
        }
        if (stored instanceof Petition storedPetition
                && candidate instanceof Petition candidatePetition) {
            return storedPetition.getId() != null
                    && storedPetition.getId().equals(candidatePetition.getId());
        }
        if (stored instanceof User storedUser && candidate instanceof User candidateUser) {
            return storedUser.getId() != null
                    && storedUser.getId().equals(candidateUser.getId());
        }
        return false;
    }

    public Long getId() { return id; }
    public Petition getPetition() { return petition; }
    public User getUser() { return user; }
    public int getAnonymousNumber() { return anonymousNumber; }
}
