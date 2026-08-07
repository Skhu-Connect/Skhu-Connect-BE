package org.skhuconnect.admin.answer.repository;

import org.skhuconnect.admin.answer.entity.OfficialAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficialAnswerRepository extends JpaRepository<OfficialAnswer, Long> {

    Optional<OfficialAnswer> findByPetitionId(Long petitionId);
}