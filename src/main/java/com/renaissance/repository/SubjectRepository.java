package com.renaissance.repository;

import com.renaissance.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCodeIgnoreCase(String code);
    List<Subject> findByDepartmentId(Long departmentId);
    List<Subject> findByDepartmentIdAndSemester(Long departmentId, Integer semester);
}
