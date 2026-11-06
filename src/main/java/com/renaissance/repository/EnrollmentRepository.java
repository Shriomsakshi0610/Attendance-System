package com.renaissance.repository;

import com.renaissance.entity.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    Optional<Enrollment> findByStudent_IdAndSubject_Id(String studentId, String subjectId);

    default Optional<Enrollment> findByStudentIdAndSubjectId(String studentId, String subjectId) {
        return findByStudent_IdAndSubject_Id(studentId, subjectId);
    }

    Optional<Enrollment> findByStudent_UsnIgnoreCaseAndSubject_Id(String usn, String subjectId);

    default Optional<Enrollment> findByStudentUsnIgnoreCaseAndSubjectId(String usn, String subjectId) {
        return findByStudent_UsnIgnoreCaseAndSubject_Id(usn, subjectId);
    }

    Optional<Enrollment> findByStudent_UsnIgnoreCaseAndCollegeClass_IdAndSubject_Id(String usn, String classId, String subjectId);

    default Optional<Enrollment> findByStudentUsnIgnoreCaseAndCollegeClassIdAndSubjectId(String usn, String classId, String subjectId) {
        return findByStudent_UsnIgnoreCaseAndCollegeClass_IdAndSubject_Id(usn, classId, subjectId);
    }

    boolean existsByStudent_UsnIgnoreCaseAndCollegeClass_Id(String usn, String classId);

    default boolean existsByStudentUsnIgnoreCaseAndCollegeClassId(String usn, String classId) {
        return existsByStudent_UsnIgnoreCaseAndCollegeClass_Id(usn, classId);
    }

    List<Enrollment> findByCollegeClass_IdAndSubject_Id(String classId, String subjectId);

    default List<Enrollment> findByCollegeClassIdAndSubjectId(String classId, String subjectId) {
        return findByCollegeClass_IdAndSubject_Id(classId, subjectId);
    }

    List<Enrollment> findByStudent_Id(String studentId);

    default List<Enrollment> findByStudentId(String studentId) {
        return findByStudent_Id(studentId);
    }

    long countByCollegeClass_Id(String classId);

    long countBySubject_Id(String subjectId);

    long countByStudent_Id(String studentId);
}
