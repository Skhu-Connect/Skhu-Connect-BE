package org.skhuconnect.petition.repository;

import jakarta.persistence.LockModeType;
import org.skhuconnect.petition.entity.Petition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long>,
        JpaSpecificationExecutor<Petition> {

    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(org.skhuconnect.petition.entity.PetitionStatus status);

    Optional<Petition> findByIdAndDeletedFalse(Long id);

    @Query("select max(petition.createdAt) from Petition petition "
            + "where petition.writer.id = :userId")
    Optional<LocalDateTime> findLatestCreatedAtByWriterId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "writer")
    Page<Petition> findByDeletedFalse(Pageable pageable);

    Optional<Petition> findByIdAndDeletedFalseAndHiddenFalse(Long id);

    /**
     * 작성자의 수정·삭제용 조회. 동의·댓글 등록이 같은 행을 PESSIMISTIC_WRITE 로 잠그므로
     * 여기서도 같은 잠금을 잡아야 한다. 잠금 없이 읽으면 "동의 0건일 때만 수정·삭제" 규칙이
     * 동시 요청에서 뚫리고, 전 컬럼 UPDATE 가 방금 증가한 agreement_count 를 되돌린다.
     * 숨김 청원은 여기서 걸러내지 않는다 - 404 가 아니라 409(수정 불가)로 답해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select petition from Petition petition
            where petition.id = :id and petition.deleted = false
            """)
    Optional<Petition> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    @Query("""
            select petition from Petition petition
            where petition.id = :petitionId and petition.deleted = false and petition.hidden = false
              and (:viewerId is null or not exists (
                  select block from UserBlock block
                  where block.blocker.id = :viewerId and block.blockedUser.id = petition.writer.id))
            """)
    Optional<Petition> findVisibleByIdForViewer(@Param("petitionId") Long petitionId,
                                                 @Param("viewerId") Long viewerId);

    @Query("""
            select petition from Petition petition
            where petition.writer.id = :userId
              and petition.deleted = false
              and petition.hidden = false
            """)
    Page<Petition> findVisibleByWriterId(
            @Param("userId") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select petition
            from Petition petition
            where petition.id = :id
              and petition.deleted = false
              and petition.hidden = false
            """)
    Optional<Petition> findVisibleByIdForUpdate(@Param("id") Long id);

    @Query("""
            select petition
            from Petition petition
            where petition.deleted = false
              and petition.hidden = false
            order by petition.id asc
            """)
    List<Petition> findPublicPetitionsForEmbedding(Pageable pageable);
}
