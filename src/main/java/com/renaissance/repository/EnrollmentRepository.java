package com.renaissance.repository;

import com.renaissance.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    Optional<Enrollment> findByStudentUsnIgnoreCaseAndSubjectId(String usn, Long subjectId);
    Optional<Enrollment> findByStudentUsnIgnoreCaseAndCollegeClassIdAndSubjectId(String usn, Long classId, Long subjectId);
    boolean existsByStudentUsnIgnoreCaseAndCollegeClassId(String usn, Long classId);
    List<Enrollment> findByCollegeClassIdAndSubjectId(Long classId, Long subjectId);
    List<Enrollment> findByStudentId(Long studentId);
}
