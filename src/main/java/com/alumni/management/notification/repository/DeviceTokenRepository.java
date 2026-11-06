package com.alumni.management.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alumni.management.notification.entity.DeviceToken;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserEmailIgnoreCaseAndIsActiveTrue(String userEmail);

    List<DeviceToken> findByUserEmailIgnoreCase(String userEmail);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE LOWER(d.userEmail) = LOWER(:userEmail) AND d.token = :token")
    void deleteByUserEmailAndToken(@Param("userEmail") String userEmail, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE LOWER(d.userEmail) = LOWER(:userEmail)")
    void deleteAllByUserEmail(@Param("userEmail") String userEmail);
}

