package com.renaissance.repository;

import com.renaissance.entity.AttendanceSession;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AttendanceSessionRepositoryCustom {

    Optional<AttendanceSession> findActiveConflictingSession(String classId, String subjectId, LocalDateTime now);

    long countActiveSessions(LocalDateTime now);

    long countSessionsToday(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
