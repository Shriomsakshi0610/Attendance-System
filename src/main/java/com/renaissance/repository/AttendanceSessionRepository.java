package com.renaissance.repository;

import com.renaissance.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findBySessionCodeIgnoreCase(String sessionCode);

    List<AttendanceSession> findByStatus(AttendanceSession.SessionStatus status);

    List<AttendanceSession> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    @Query("SELECT s FROM AttendanceSession s WHERE s.collegeClass.id = :classId AND s.subject.id = :subjectId AND s.status = 'ACTIVE' AND s.endTime > :now")
    Optional<AttendanceSession> findActiveConflictingSession(@Param("classId") Long classId,
                                                           @Param("subjectId") Long subjectId,
                                                           @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.status = 'ACTIVE' AND s.endTime > :now")
    long countActiveSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.status != 'EXPIRED' AND s.createdAt >= :startOfDay AND s.createdAt <= :endOfDay")
    long countSessionsToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    List<AttendanceSession> findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(Long classId, Long subjectId);

    List<AttendanceSession> findByCollegeClassIdOrderByStartTimeDesc(Long classId);
}
