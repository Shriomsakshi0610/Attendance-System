package com.renaissance.repository;

import com.renaissance.entity.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends MongoRepository<Student, String>, StudentRepositoryCustom {

    Optional<Student> findByUsnIgnoreCase(String usn);

    boolean existsByUsnIgnoreCase(String usn);

    List<Student> findByCollegeClass_Id(String classId);

    default List<Student> findByCollegeClassId(String classId) {
        return findByCollegeClass_Id(classId);
    }

    List<Student> findByCollegeClass_IdIn(java.util.Collection<String> classIds);

    default List<Student> findByCollegeClassIdIn(java.util.Collection<String> classIds) {
        return findByCollegeClass_IdIn(classIds);
    }

    List<Student> findByDepartment_Id(String departmentId);

    default List<Student> findByDepartmentId(String departmentId) {
        return findByDepartment_Id(departmentId);
    }

    long countByDepartment_Id(String departmentId);

    default long countByDepartmentId(String departmentId) {
        return countByDepartment_Id(departmentId);
    }

    long countByCollegeClass_Id(String classId);

    default long countByCollegeClassId(String classId) {
        return countByCollegeClass_Id(classId);
    }

    List<Student> findByCollegeClass_IdAndStatus(String classId, String status);

    List<Student> findByStatus(String status);
}
