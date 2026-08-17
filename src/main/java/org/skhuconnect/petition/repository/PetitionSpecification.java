package org.skhuconnect.petition.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.skhuconnect.petition.dto.request.PetitionQueryCondition;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PetitionSpecification {

    private PetitionSpecification() {
    }

    public static Specification<Petition> query(PetitionQueryCondition condition, LocalDateTime now) {
        return query(condition, now, null);
    }

    public static Specification<Petition> query(
            PetitionQueryCondition condition,
            LocalDateTime now,
            Long viewerId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            predicates.add(criteriaBuilder.isFalse(root.get("hidden")));
            if (viewerId != null) {
                var blocked = query.subquery(Long.class);
                var userBlock = blocked.from(org.skhuconnect.user.block.entity.UserBlock.class);
                blocked.select(userBlock.get("id"));
                blocked.where(
                        criteriaBuilder.equal(userBlock.get("blocker").get("id"), viewerId),
                        criteriaBuilder.equal(userBlock.get("blockedUser").get("id"), root.get("writer").get("id")));
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(blocked)));
            }

            String keyword = condition.normalizedKeyword();
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("content")), pattern)
                ));
            }
            if (condition.category() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category"), condition.category()));
            }
            addStatusPredicate(predicates, condition.status(), now, root, criteriaBuilder);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addStatusPredicate(
            List<Predicate> predicates,
            PetitionStatus status,
            LocalDateTime now,
            Root<Petition> root,
            CriteriaBuilder criteriaBuilder
    ) {
        if (status == null) {
            return;
        }

        Predicate calculatedExpired = criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), PetitionStatus.OPEN),
                criteriaBuilder.lessThan(root.<LocalDateTime>get("agreementDeadline"), now),
                criteriaBuilder.lessThan(
                        root.<Integer>get("agreementCount"),
                        root.<Integer>get("targetAgreementCount"))
        );
        if (status == PetitionStatus.EXPIRED) {
            predicates.add(calculatedExpired);
            return;
        }
        if (status == PetitionStatus.OPEN) {
            predicates.add(criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("status"), PetitionStatus.OPEN),
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.<LocalDateTime>get("agreementDeadline"), now)
            ));
            return;
        }
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
    }
}
