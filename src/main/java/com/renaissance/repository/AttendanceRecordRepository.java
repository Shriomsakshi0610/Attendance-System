package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends MongoRepository<AttendanceRecord, String>, AttendanceRecordRepositoryCustom {

    List<AttendanceRecord> findBySession_IdOrderByMarkedAtDesc(String sessionId);

    default List<AttendanceRecord> findBySessionIdOrderByMarkedAtDesc(String sessionId) {
        return findBySession_IdOrderByMarkedAtDesc(sessionId);
    }

    Optional<AttendanceRecord> findBySession_IdAndStudent_Id(String sessionId, String studentId);

    default Optional<AttendanceRecord> findBySessionIdAndStudentId(String sessionId, String studentId) {
        return findBySession_IdAndStudent_Id(sessionId, studentId);
    }

    Optional<AttendanceRecord> findBySession_IdAndStudent_UsnIgnoreCase(String sessionId, String usn);

    default Optional<AttendanceRecord> findBySessionIdAndStudentUsnIgnoreCase(String sessionId, String usn) {
        return findBySession_IdAndStudent_UsnIgnoreCase(sessionId, usn);
    }

    boolean existsBySession_IdAndStudent_Id(String sessionId, String studentId);

    default boolean existsBySessionIdAndStudentId(String sessionId, String studentId) {
        return existsBySession_IdAndStudent_Id(sessionId, studentId);
    }

    boolean existsBySession_IdAndStudent_UsnIgnoreCase(String sessionId, String usn);

    default boolean existsBySessionIdAndStudentUsnIgnoreCase(String sessionId, String usn) {
        return existsBySession_IdAndStudent_UsnIgnoreCase(sessionId, usn);
    }

    List<AttendanceRecord> findByStudent_IdOrderByMarkedAtDesc(String studentId);

    default List<AttendanceRecord> findByStudentIdOrderByMarkedAtDesc(String studentId) {
        return findByStudent_IdOrderByMarkedAtDesc(studentId);
    }

    List<AttendanceRecord> findByStudent_UsnIgnoreCaseOrderByMarkedAtDesc(String usn);

    default List<AttendanceRecord> findByStudentUsnIgnoreCaseOrderByMarkedAtDesc(String usn) {
        return findByStudent_UsnIgnoreCaseOrderByMarkedAtDesc(usn);
    }

    long countBySession_Id(String sessionId);

    default long countBySessionId(String sessionId) {
        return countBySession_Id(sessionId);
    }

    long countByStudent_Id(String studentId);

    default long countByStudentId(String studentId) {
        return countByStudent_Id(studentId);
    }

    long countBySession_IdAndStatus(String sessionId, AttendanceRecord.AttendanceStatus status);

    default long countBySessionIdAndStatus(String sessionId, AttendanceRecord.AttendanceStatus status) {
        return countBySession_IdAndStatus(sessionId, status);
    }

    long countByStatus(AttendanceRecord.AttendanceStatus status);
}
