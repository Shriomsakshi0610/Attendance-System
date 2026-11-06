package com.renaissance.repository;

import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<CollegeClass, Long> {
    List<CollegeClass> findByDepartmentId(Long departmentId);
    List<CollegeClass> findByDepartmentIdAndSemester(Long departmentId, Integer semester);
    Optional<CollegeClass> findByDepartmentAndSemesterAndSection(Department department, Integer semester, String section);
    Optional<CollegeClass> findByDepartmentIdAndSemesterAndSection(Long departmentId, Integer semester, String section);
}
