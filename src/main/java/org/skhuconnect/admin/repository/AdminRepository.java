package org.skhuconnect.admin.repository;

import jakarta.persistence.LockModeType;
import org.skhuconnect.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Admin a where a.loginId = :loginId")
    Optional<Admin> findByLoginIdForUpdate(@Param("loginId") String loginId);
}