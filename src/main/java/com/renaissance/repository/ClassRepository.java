package com.renaissance.repository;

import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends MongoRepository<CollegeClass, String> {

    List<CollegeClass> findByDepartment_Id(String departmentId);

    default List<CollegeClass> findByDepartmentId(String departmentId) {
        return findByDepartment_Id(departmentId);
    }

    List<CollegeClass> findByDepartment_IdAndSemester(String departmentId, Integer semester);

    default List<CollegeClass> findByDepartmentIdAndSemester(String departmentId, Integer semester) {
        return findByDepartment_IdAndSemester(departmentId, semester);
    }

    Optional<CollegeClass> findByDepartmentAndSemesterAndSection(Department department, Integer semester, String section);

    Optional<CollegeClass> findByDepartment_IdAndSemesterAndSection(String departmentId, Integer semester, String section);

    default Optional<CollegeClass> findByDepartmentIdAndSemesterAndSection(String departmentId, Integer semester, String section) {
        return findByDepartment_IdAndSemesterAndSection(departmentId, semester, section);
    }

    boolean existsByDepartment_IdAndSemesterAndSectionIgnoreCase(String departmentId, Integer semester, String section);

    long countByDepartment_Id(String departmentId);

    long countByClassTeacher_Id(String teacherId);

    List<CollegeClass> findByClassTeacher_Id(String teacherId);

    List<CollegeClass> findByStatus(String status);
}
