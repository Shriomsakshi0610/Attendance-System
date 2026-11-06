package com.renaissance.repository;

import com.renaissance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUsnIgnoreCase(String usn);
    boolean existsByUsnIgnoreCase(String usn);
    List<Student> findByCollegeClassId(Long classId);
    List<Student> findByDepartmentId(Long departmentId);

    @Query("SELECT s FROM Student s WHERE " +
           "(:deptId IS NULL OR s.department.id = :deptId) AND " +
           "(:classId IS NULL OR s.collegeClass.id = :classId) AND " +
           "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.usn) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Student> searchStudents(@Param("deptId") Long deptId,
                                @Param("classId") Long classId,
                                @Param("keyword") String keyword);
}
