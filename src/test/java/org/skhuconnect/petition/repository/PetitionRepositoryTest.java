package org.skhuconnect.petition.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.dto.request.PetitionQueryCondition;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class PetitionRepositoryTest {

    @Autowired
    private PetitionRepository petitionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void savesAndFindsNonDeletedPetition() {
        User writer = saveWriter();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);

        Petition saved = petitionRepository.saveAndFlush(Petition.create(
                writer,
                PetitionCategory.SCHOLARSHIP,
                "장학금 제도 개선",
                "장학금 제도를 개선해주세요.",
                10,
                now
        ));

        Petition found = petitionRepository.findByIdAndDeletedFalse(saved.getId())
                .orElseThrow();
        assertThat(found.getWriter().getId()).isEqualTo(writer.getId());
        assertThat(found.getTitle()).isEqualTo("장학금 제도 개선");
        assertThat(found.getAgreementDeadline()).isEqualTo(now.plusDays(30));
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void excludesSoftDeletedPetition() {
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                saveWriter(),
                PetitionCategory.LIBRARY,
                "도서관 개선",
                "도서관을 개선해주세요.",
                5,
                LocalDateTime.now()
        ));

        petition.delete(LocalDateTime.now());
        petitionRepository.flush();

        assertThat(petitionRepository.findByIdAndDeletedFalse(petition.getId()))
                .isEmpty();
        assertThat(petitionRepository.findById(petition.getId()))
                .hasValueSatisfying(found -> assertThat(found.isDeleted()).isTrue());
    }

    @Test
    void openAndExpiredFiltersDoNotOverlap() {
        User writer = saveWriter();
        String keyword = "status-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        Petition activeOpen = petitionRepository.save(Petition.create(
                writer, PetitionCategory.FACILITY, keyword + "-active", "active content",
                10, now.minusDays(10)));
        Petition expiredOpen = petitionRepository.save(Petition.create(
                writer, PetitionCategory.FACILITY, keyword + "-expired", "expired content",
                10, now.minusDays(31)));
        Petition thresholdReached = Petition.create(
                writer, PetitionCategory.FACILITY, keyword + "-reached", "reached content",
                10, now.minusDays(31));
        ReflectionTestUtils.setField(thresholdReached, "agreementCount", 10);
        petitionRepository.saveAndFlush(thresholdReached);

        var open = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        keyword, null, PetitionStatus.OPEN), now),
                Pageable.unpaged());
        var expired = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        keyword, null, PetitionStatus.EXPIRED), now),
                Pageable.unpaged());

        assertThat(open.getContent()).extracting(Petition::getId)
                .containsExactly(activeOpen.getId())
                .doesNotContain(expiredOpen.getId());
        assertThat(expired.getContent()).extracting(Petition::getId)
                .containsExactly(expiredOpen.getId());
        assertThat(open.getContent()).extracting(Petition::getId)
                .doesNotContainAnyElementsOf(
                        expired.getContent().stream().map(Petition::getId).toList());
    }

    @Test
    void queryExcludesHiddenAndDeletedAndSearchesTitleOrContent() {
        User writer = saveWriter();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        Petition titleMatch = petitionRepository.save(Petition.create(
                writer, PetitionCategory.LIBRARY, "reading room", "content",
                10, now));
        Petition contentMatch = petitionRepository.save(Petition.create(
                writer, PetitionCategory.LIBRARY, "title", "reading room extension",
                10, now));
        Petition hidden = Petition.create(
                writer, PetitionCategory.LIBRARY, "reading room hidden", "content",
                10, now);
        ReflectionTestUtils.setField(hidden, "hidden", true);
        petitionRepository.save(hidden);
        Petition deleted = Petition.create(
                writer, PetitionCategory.LIBRARY, "reading room deleted", "content",
                10, now);
        deleted.delete(now);
        petitionRepository.saveAndFlush(deleted);

        var result = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        "reading room", PetitionCategory.LIBRARY, null), now),
                Pageable.unpaged());

        assertThat(result.getContent()).extracting(Petition::getId)
                .containsExactlyInAnyOrder(titleMatch.getId(), contentMatch.getId());
        assertThat(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(
                hidden.getId())).isEmpty();
        assertThat(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(
                deleted.getId())).isEmpty();
    }
    @Test
    void statusBoundaryAndStoredStatusesAreFilteredExactly() {
        User writer = saveWriter();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        String keyword = "status-boundary-" + UUID.randomUUID();
        Petition boundaryOpen = petitionRepository.save(Petition.create(
                writer, PetitionCategory.SCHOLARSHIP, keyword + "-boundary", "content",
                10, now.minusDays(30)));
        Petition underReview = Petition.create(
                writer, PetitionCategory.SCHOLARSHIP, keyword + "-review", "content",
                10, now);
        ReflectionTestUtils.setField(
                underReview, "status", PetitionStatus.UNDER_REVIEW);
        petitionRepository.save(underReview);
        Petition answered = Petition.create(
                writer, PetitionCategory.SCHOLARSHIP, keyword + "-answered", "content",
                10, now);
        ReflectionTestUtils.setField(answered, "status", PetitionStatus.ANSWERED);
        petitionRepository.saveAndFlush(answered);

        var open = petitionRepository.findAll(PetitionSpecification.query(
                new PetitionQueryCondition(keyword, null, PetitionStatus.OPEN), now));
        var expired = petitionRepository.findAll(PetitionSpecification.query(
                new PetitionQueryCondition(keyword, null, PetitionStatus.EXPIRED), now));
        var review = petitionRepository.findAll(PetitionSpecification.query(
                new PetitionQueryCondition(
                        keyword, null, PetitionStatus.UNDER_REVIEW), now));
        var answer = petitionRepository.findAll(PetitionSpecification.query(
                new PetitionQueryCondition(keyword, null, PetitionStatus.ANSWERED), now));

        assertThat(open).extracting(Petition::getId)
                .contains(boundaryOpen.getId());
        assertThat(expired).extracting(Petition::getId)
                .doesNotContain(boundaryOpen.getId());
        assertThat(review).extracting(Petition::getId)
                .containsExactly(underReview.getId());
        assertThat(answer).extracting(Petition::getId)
                .containsExactly(answered.getId());
    }

    @Test
    void combinedSearchTrimsKeywordIgnoresCaseAndCanReturnEmptyPage() {
        User writer = saveWriter();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        Petition match = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.DORMITORY,
                "Dormitory LIGHT", "extension request", 10, now));

        var result = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        "  dormitory light  ",
                        PetitionCategory.DORMITORY,
                        PetitionStatus.OPEN), now),
                PageRequest.of(0, 10));
        var empty = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        "missing", PetitionCategory.DORMITORY,
                        PetitionStatus.OPEN), now),
                PageRequest.of(0, 10));
        var blankKeyword = petitionRepository.findAll(
                PetitionSpecification.query(new PetitionQueryCondition(
                        "   ", PetitionCategory.DORMITORY,
                        PetitionStatus.OPEN), now),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Petition::getId)
                .containsExactly(match.getId());
        assertThat(empty.getContent()).isEmpty();
        assertThat(empty.getTotalElements()).isZero();
        assertThat(blankKeyword.getContent()).extracting(Petition::getId)
                .contains(match.getId());
    }

    @Test
    void stableSecondaryIdSortPreventsPageOverlap() {
        User writer = saveWriter();
        String keyword = "page-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        for (int index = 0; index < 5; index++) {
            Petition petition = Petition.create(
                    writer, PetitionCategory.FACILITY,
                    keyword + "-same-count-" + index, "content", 10, now);
            ReflectionTestUtils.setField(petition, "agreementCount", 1);
            petitionRepository.save(petition);
        }
        petitionRepository.flush();
        var specification = PetitionSpecification.query(
                new PetitionQueryCondition(keyword, null, null), now);
        Sort sort = Sort.by(
                Sort.Order.desc("agreementCount"),
                Sort.Order.desc("id"));

        var first = petitionRepository.findAll(
                specification, PageRequest.of(0, 2, sort));
        var second = petitionRepository.findAll(
                specification, PageRequest.of(1, 2, sort));
        var last = petitionRepository.findAll(
                specification, PageRequest.of(2, 2, sort));
        List<Long> ids = new java.util.ArrayList<>();
        first.forEach(petition -> ids.add(petition.getId()));
        second.forEach(petition -> ids.add(petition.getId()));
        last.forEach(petition -> ids.add(petition.getId()));

        assertThat(ids).hasSize(5);
        assertThat(new HashSet<>(ids)).hasSize(5);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(last.isLast()).isTrue();
    }
    private User saveWriter() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "D" + unique.substring(0, 12),
                "테스트학과-" + unique.substring(0, 12)
        ));
        return userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr",
                "user" + unique.substring(0, 12),
                "encoded-password",
                department
        ));
    }
}
