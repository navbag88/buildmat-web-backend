package com.buildmat.repository;

import com.buildmat.model.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    Optional<UserSessionEntity> findBySessionTokenAndActiveTrue(String sessionToken);

    List<UserSessionEntity> findByUserIdAndActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.active = false WHERE s.userId = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.active = false WHERE s.sessionToken = :token")
    void deactivateByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
