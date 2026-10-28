package com.alumni.management.verification.repository;

import com.alumni.management.verification.entity.EmailVerification;
import com.alumni.management.verification.enums.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndPurpose(
            String email,
            VerificationPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email AND v.purpose = :purpose")
    Optional<EmailVerification> findByEmailAndPurposeForUpdate(
            @Param("email") String email,
            @Param("purpose") VerificationPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email AND v.purpose IN :purposes ORDER BY v.lastSentAt DESC")
    List<EmailVerification> findByEmailAndPurposesForUpdate(
            @Param("email") String email,
            @Param("purposes") Collection<VerificationPurpose> purposes
    );

    void deleteByEmailAndPurpose(String email, VerificationPurpose purpose);
}
