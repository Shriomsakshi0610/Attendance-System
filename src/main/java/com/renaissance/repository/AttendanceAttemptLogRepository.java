package com.renaissance.repository;

import com.renaissance.entity.AttendanceAttemptLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAttemptLogRepository extends MongoRepository<AttendanceAttemptLog, String> {

    List<AttendanceAttemptLog> findBySessionIdOrderByAttemptTimeDesc(String sessionId);

    List<AttendanceAttemptLog> findByUsnIgnoreCaseOrderByAttemptTimeDesc(String usn);

    List<AttendanceAttemptLog> findTop50ByOrderByAttemptTimeDesc();
}
