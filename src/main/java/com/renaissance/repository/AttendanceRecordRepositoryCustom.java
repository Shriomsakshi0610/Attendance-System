package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AttendanceRecordRepositoryCustom {

    long countRecordsToday(LocalDateTime startOfDay, LocalDateTime endOfDay);

    long countPresentByStudentId(String studentId);

    long countPresentByStudentIdAndSubjectId(String studentId, String subjectId);

    Map<String, Long> countPresentByStudentsInClass(String classId, String subjectId);

    Map<String, Long> countRecordsByDay(LocalDateTime startDateTime, LocalDateTime endDateTime);

    Map<String, Long> countRecordsBySessionIds(java.util.Collection<String> sessionIds);

    Map<String, Long> countPresentBySessionIds(java.util.Collection<String> sessionIds);

    List<AttendanceRecord> findFilteredRecords(String classId, String subjectId, LocalDateTime startDate, LocalDateTime endDate);
}
