package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionIdOrderByMarkedAtDesc(Long sessionId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudentUsnIgnoreCase(Long sessionId, String usn);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionIdAndStudentUsnIgnoreCase(Long sessionId, String usn);

    List<AttendanceRecord> findByStudentIdOrderByMarkedAtDesc(Long studentId);

    List<AttendanceRecord> findByStudentUsnIgnoreCaseOrderByMarkedAtDesc(String usn);

    long countBySessionId(Long sessionId);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.markedAt >= :startOfDay AND r.markedAt <= :endOfDay")
    long countRecordsToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.student.id = :studentId AND r.status = 'PRESENT'")
    long countPresentByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.student.id = :studentId AND r.session.subject.id = :subjectId AND r.status = 'PRESENT'")
    long countPresentByStudentIdAndSubjectId(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT r FROM AttendanceRecord r WHERE r.session.collegeClass.id = :classId " +
           "AND (:subjectId IS NULL OR r.session.subject.id = :subjectId) " +
           "AND (:startDate IS NULL OR r.markedAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.markedAt <= :endDate)")
    List<AttendanceRecord> findFilteredRecords(@Param("classId") Long classId,
                                              @Param("subjectId") Long subjectId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}
