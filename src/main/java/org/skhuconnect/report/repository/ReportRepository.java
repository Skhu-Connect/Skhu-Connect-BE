package org.skhuconnect.report.repository;
import org.skhuconnect.report.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ReportRepository extends JpaRepository<Report,Long>{
 boolean existsByReporterIdAndPetitionId(Long reporterId,Long petitionId);
 boolean existsByReporterIdAndCommentId(Long reporterId,Long commentId);
 @EntityGraph(attributePaths={"reporter","petition","comment","processedByAdmin"})
 Page<Report> findAll(Pageable pageable);
 @EntityGraph(attributePaths={"reporter","petition","comment","processedByAdmin"})
 Page<Report> findByStatus(ReportStatus status,Pageable pageable);
 @EntityGraph(attributePaths={"reporter","petition","comment","processedByAdmin"})
 Page<Report> findByTargetType(ReportTargetType type,Pageable pageable);
 @EntityGraph(attributePaths={"reporter","petition","comment","processedByAdmin"})
 Page<Report> findByStatusAndTargetType(ReportStatus status,ReportTargetType type,Pageable pageable);
 @EntityGraph(attributePaths={"reporter","petition","comment","processedByAdmin"})
 Optional<Report> findById(Long id);
}