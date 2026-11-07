package com.renaissance.repository;

import com.renaissance.entity.AttendanceSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends MongoRepository<AttendanceSession, String>, AttendanceSessionRepositoryCustom {

    Optional<AttendanceSession> findBySessionCodeIgnoreCase(String sessionCode);

    List<AttendanceSession> findByStatus(AttendanceSession.SessionStatus status);

    List<AttendanceSession> findByTeacher_IdOrderByCreatedAtDesc(String teacherId);

    default List<AttendanceSession> findByTeacherIdOrderByCreatedAtDesc(String teacherId) {
        return findByTeacher_IdOrderByCreatedAtDesc(teacherId);
    }

    List<AttendanceSession> findByCollegeClass_IdAndSubject_IdOrderByStartTimeAsc(String classId, String subjectId);

    default List<AttendanceSession> findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(String classId, String subjectId) {
        return findByCollegeClass_IdAndSubject_IdOrderByStartTimeAsc(classId, subjectId);
    }

    List<AttendanceSession> findByCollegeClass_IdOrderByStartTimeDesc(String classId);

    default List<AttendanceSession> findByCollegeClassIdOrderByStartTimeDesc(String classId) {
        return findByCollegeClass_IdOrderByStartTimeDesc(classId);
    }

    List<AttendanceSession> findByCollegeClass_IdIn(java.util.Collection<String> classIds);

    default List<AttendanceSession> findByCollegeClassIdIn(java.util.Collection<String> classIds) {
        return findByCollegeClass_IdIn(classIds);
    }

    long countByCollegeClass_Id(String classId);

    long countBySubject_Id(String subjectId);

    long countByTeacher_Id(String teacherId);

    long countByStatus(AttendanceSession.SessionStatus status);
}
