package com.renaissance.repository;

import com.renaissance.entity.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends MongoRepository<Subject, String> {

    Optional<Subject> findByCodeIgnoreCase(String code);

    List<Subject> findByDepartment_Id(String departmentId);

    default List<Subject> findByDepartmentId(String departmentId) {
        return findByDepartment_Id(departmentId);
    }

    List<Subject> findByDepartment_IdAndSemester(String departmentId, Integer semester);

    default List<Subject> findByDepartmentIdAndSemester(String departmentId, Integer semester) {
        return findByDepartment_IdAndSemester(departmentId, semester);
    }

    long countByDepartment_Id(String departmentId);

    long countByAssignedTeacher_Id(String teacherId);

    long countByCollegeClass_Id(String classId);

    List<Subject> findByAssignedTeacher_Id(String teacherId);

    List<Subject> findByCollegeClass_Id(String classId);

    List<Subject> findByStatus(String status);
}
