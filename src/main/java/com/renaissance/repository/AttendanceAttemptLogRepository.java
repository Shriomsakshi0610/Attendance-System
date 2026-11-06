package com.renaissance.repository;

import com.renaissance.entity.AttendanceAttemptLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAttemptLogRepository extends JpaRepository<AttendanceAttemptLog, Long> {
    List<AttendanceAttemptLog> findBySessionIdOrderByAttemptTimeDesc(Long sessionId);
    List<AttendanceAttemptLog> findByUsnIgnoreCaseOrderByAttemptTimeDesc(String usn);
    List<AttendanceAttemptLog> findTop50ByOrderByAttemptTimeDesc();
}
