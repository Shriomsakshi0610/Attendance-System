package com.renaissance.repository;

import com.renaissance.entity.AttendanceRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRecordRepositoryCustom {

    long countRecordsToday(LocalDateTime startOfDay, LocalDateTime endOfDay);

    long countPresentByStudentId(String studentId);

    long countPresentByStudentIdAndSubjectId(String studentId, String subjectId);

    List<AttendanceRecord> findFilteredRecords(String classId, String subjectId, LocalDateTime startDate, LocalDateTime endDate);
}
